package nxt.db.quicksync;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import nxt.db.derby.DerbyDbs;
import nxt.db.firebird.FirebirdDbs;
import nxt.db.h2.H2Dbs;
import nxt.db.mariadb.MariadbDbs;
import nxt.db.sql.Db;
import nxt.db.store.Dbs;
import nxt.util.LoggerConfigurator;
import org.apache.commons.lang.StringUtils;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Loads a binary dump created with @see({@link LoadBinDump}.
 * The source can either be a file on the local filesystem or a http-url to a remote file.
 * The latter is then downloaded into the temp directory and deleted afterwards.
 * <p>
 * The method used here is basically the reverse of what is described in {@link LoadBinDump}.
 * Reflection is used to analyze the pojos in the pojos-package, insert-statements are created from
 * the gathered information and the pojos are read from the dump.
 * <p>
 * Then jdbc-batch-statements are used to insert the data into the database.
 * Contents of the db are truncated before inserting (table by table).
 * Everything takes place in a transaction.
 * <p>
 * by BraindeadOne (BURST-BJSX-4C6A-UH35-F4Q3A)
 */

public class LoadBinDump {
    private static final Logger logger = LoggerFactory.getLogger(LoadBinDump.class.getSimpleName());
    private static final int VERSION = 1;
    private static Dbs dbs;

    public static void main(String[] args) {
        Path temp = null;
        try {
            if (args.length == 0) {
                System.err.println("Please provide a filename or url as parameter");
                System.exit(-2);
            }

            boolean automatic = args.length > 1 && args[1].equals("-y");
            if (!automatic) {
                System.out.println("" +
                        "==================================================\n" +
                        "This (re)builds your database from a binary dump.\n" +
                        "Since the contained transactions are not checked\n" +
                        "for validity you should only use a dump from a \n" +
                        "trusted source.\n" +
                        "\n" +
                        "ALL EXISTING DATA IN YOUR DATABASE WILL BE DELETED\n" +
                        "==================================================\n" +
                        "Make sure no wallet is accessing the database\n\n" +
                        "Do you want to continue? "
                );
                int c = System.in.read();
                if (c != 'y' && c != 'Y' && c != 'j' && c != 'J') {
                    System.out.println("Ok then");
                    System.exit(-1);
                }
            }
            Path source = null;

            if (args[0].startsWith("http://") || args[0].startsWith("https://")) {
                URL url = new URL(args[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setInstanceFollowRedirects(true);
                System.out.println("Downloading " + url.toString() + "...");
                if (httpURLConnection.getResponseCode() == 200) {
                    temp = Files.createTempFile("BuRST", ".dump.gz");
                    source = temp;
                    try (InputStream inputStream = httpURLConnection.getInputStream(); OutputStream outputStream = new FileOutputStream(temp.toFile())) {
                        byte buf[] = new byte[1024 * 1024 * 50];
                        int read = 0;
                        long totalRead = 0;
                        long lastTotal = 0;
                        while ((read = inputStream.read(buf, 0, buf.length)) >= 0) {
                            outputStream.write(buf, 0, read);
                            totalRead += read;
                            if (totalRead - lastTotal > 1024 * 1024 * 10) {
                                System.out.println(String.format("%d MB read      \r", totalRead / 1024 / 1024));
                                lastTotal = totalRead;
                            }
                        }
                        System.out.println(String.format("%d MB read\nDone", totalRead / 1024 / 1024));
                    }
                    System.err.println("");
                } else {
                    System.err.println("Error downloading file (RC " + httpURLConnection.getResponseCode() + " - " + httpURLConnection.getResponseMessage() + ")");
                    System.exit(6);
                }
            } else {
                source = Paths.get(args[0]);
                if (!Files.exists(source) || !Files.isReadable(source)) {
                    System.err.println("Source file " + source.toAbsolutePath().toString() + " does not exist or is not readable.");
                    System.exit(-3);
                }
            }
            LoggerConfigurator.init();
            // These options lead to problems in normal use but bring a extreme performance boost here
            Db.addDatabaseOption(Db.TYPE.MARIADB, "useServerPrepStmts", "false");
            Db.addDatabaseOption(Db.TYPE.MARIADB, "rewriteBatchedStatements", "true");
            Db.init();
            switch (Db.getDatabaseType()) {
                case MARIADB:
                    logger.info("Using mariadb Backend");
                    dbs = new MariadbDbs();
                    break;
                case H2:
                    logger.info("Using h2 Backend");
                    dbs = new H2Dbs();
                    break;
                case FIREBIRD:
                    logger.info("Using firebird Backend");
                    dbs = new FirebirdDbs();
                    break;
                case DERBY:
                    dbs = new DerbyDbs();
                    break;
                default:
                    throw new RuntimeException("Error initializing wallet: Unknown database type");
            }
            load(source);
        } catch (Exception e) {
            logger.error("Error", e);
        } finally {
            if (temp != null)
                try {
                    Files.delete(temp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (Db.getDatabaseType() == Db.TYPE.H2)
                logger.warn("Compacting the h2 database may take a small eternity - sorry");
            Db.shutdown();
        }
    }

    public static void load(Path path) throws
            IOException, URISyntaxException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        Kryo kryo = new Kryo();
        long start = System.currentTimeMillis();
        try (Input input = new Input(new GZIPInputStream(new FileInputStream(path.toFile())))) {
            if (!BinDumps.MAGIC.equals(input.readString())) {
                logger.error("Input file does not seem to be a blockchain dump");
                logger.error("Import aborted - no data has been changed");
                System.exit(666);
            }
            int version = input.read();
            if (version != BinDumps.VERSION) {
                logger.error("Unsupported version in source file: Expected " + BinDumps.VERSION + " but got " + version);
                logger.error("Import aborted - no data has been changed");
                System.exit(666);
            }
            logger.debug("Format version is " + version);
            logger.info("Dump was created with version " + input.readString());
            logger.trace("Blockchain height is " + input.read());

            try (Connection con = Db.getConnection()) {
                Db.beginTransaction();
                dbs.disableForeignKeyChecks(con);

                Object o = null;
                Class clazz = null;
                Statement stmt = con.createStatement();

                while (!input.eof() && (clazz = kryo.readClass(input).getType()) != null) {
                    long rows = input.readLong();
                    String sql = "truncate table " + BinDumps.getTableName(clazz) + "";
                    if (Db.getDatabaseType() == Db.TYPE.FIREBIRD)
                        sql = "delete from " + BinDumps.getTableName(clazz) + "";
                    logger.debug(sql);
                    stmt.executeUpdate(sql);


                    StringBuilder sb = new StringBuilder("insert into ");
                    sb.append(BinDumps.getTableName(clazz));
                    sb.append(" (");
                    List<Field> fields = new ArrayList<>();

                    for (Field field : ReflectionUtils.getAllFields(clazz)) {
                        fields.add(field);
                        sb.append(BinDumps.getColumnName(field)).append(",");

                    }
                    boolean hasDbId = false;
                    for (Field field : fields) {
                        String columnName = BinDumps.getColumnName(field);

                        if ("db_Id".equals(columnName))
                            hasDbId = true;
                    }

                    // Remove last ,
                    sb.deleteCharAt(sb.lastIndexOf(","));
                    sb.append(") VALUES ( ");
                    sb.append(StringUtils.repeat("?", ",", fields.size()));
                    sb.append(")");
                    sql = sb.toString();
                    logger.debug(sql);
                    PreparedStatement ps = con.prepareStatement(sql);
                    long maxId = 0;
                    for (long l = 0; l < rows; l++) {
                        o = kryo.readObject(input, clazz);
                        int i = 0;
                        for (Field field : fields) {
                            i++;
                            Class fieldType = field.getType();
                            field.setAccessible(true);

                            if (fieldType.equals(String.class)) {
                                ps.setString(i, (String) field.get(o));

                            } else if (fieldType.equals(Long.class)) {
                                Object val = field.get(o);
                                if (val == null)
                                    ps.setNull(i, Types.NUMERIC);
                                else
                                    ps.setLong(i, (Long) val);
                            } else if (fieldType.equals(long.class)) {
                                ps.setLong(i, field.getLong(o));
                            } else if (fieldType.isArray()) {
                                // Byte array?

                                if (fieldType.getComponentType().equals(byte.class)) {
                                    ps.setBytes(i, (byte[]) field.get(o));

                                } else {
                                    logger.error("Unhandled field type for" + field.getName() + ": " + fieldType);
                                }

                            } else {
                                logger.error("Unhandled field type for" + field.getName() + ": " + fieldType);
                            }
                        }
                        ps.addBatch();
                        ps.clearParameters();
                        if (l % 10000 == 0) {
                            logger.info(clazz.getSimpleName() + ": " + l + " / " + rows);
                            ps.executeBatch();
                            if (l % 50000 == 0 && Db.getDatabaseType() == Db.TYPE.FIREBIRD)
                                Db.commitTransaction();
                        }
                    }
                    ps.executeBatch();
                    logger.info(clazz.getSimpleName() + ": " + rows + " / " + rows);
                    if (hasDbId)
                    {
                        stmt.executeQuery("alter table "+BinDumps.getTableName(clazz)+" ALTER db_id  RESTART WITH ");
                    }

                }

                dbs.enableForeignKeyChecks(con);
                Db.commitTransaction();
                Db.endTransaction();
                logger.info("Dump loaded in " + ((System.currentTimeMillis() - start) / 1000) + "seconds");
            }
        }
    }
}
