package nxt.db.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public final class DbUtils {

    private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);
    private DbUtils() {
    } // never

    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public static void setBytes(PreparedStatement pstmt, int index, byte[] bytes) throws SQLException {
        if (bytes != null) {
            pstmt.setBytes(index, bytes);
        } else {
            pstmt.setNull(index, Types.BINARY);
        }
    }

    public static void setString(PreparedStatement pstmt, int index, String s) throws SQLException {
        if (s != null) {
            pstmt.setString(index, s);
        } else {
            pstmt.setNull(index, Types.VARCHAR);
        }
    }

    public static void setIntZeroToNull(PreparedStatement pstmt, int index, int n) throws SQLException {
        if (n != 0) {
            pstmt.setInt(index, n);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }

    public static void setLongZeroToNull(PreparedStatement pstmt, int index, long l) throws SQLException {
        if (l != 0) {
            pstmt.setLong(index, l);
        } else {
            pstmt.setNull(index, Types.BIGINT);
        }
    }

    public static String quoteTableName(String table) {
	 switch (Db.getDatabaseType()) {
            case FIREBIRD:
                return table.equalsIgnoreCase("at") ? "\"" + table.toUpperCase() + "\"" : table;
         case DERBY:
             if ("transaction".equalsIgnoreCase(table))
                 return "txn";
             if ("at".equalsIgnoreCase(table))
                 return "atxn";
            default:
                return table;
        }
    }

    public static String limitsClause(int limit) {
        switch (Db.getDatabaseType()) {
            case FIREBIRD:
                return " ROWS ? ";
            case DERBY:
                return " FETCH FIRST ? ROWS ONLY";
            default:
                return " LIMIT ?";
        }
    }

    public static String limitsClause(int from, int to) {
        int limit = to >= 0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;
        switch (Db.getDatabaseType()) {
            case FIREBIRD: {
                if (limit > 0 && from > 0) {
                    return " ROWS ? TO ? ";
                } else if (limit > 0) {
                    return " ROWS ? ";
                } else if (from > 0) {
                    return " ROWS ? TO ? ";
                } else {
                    return "";
                }
            }
            case DERBY:
                if (limit > 0 && from > 0) {
                    return " FETCH FIRST  ? ROWS ONLY OFFSET ? ROWS ";
                } else if (limit > 0) {
                    return " FETCH FIRST ? ROWS ONLY ";
                } else if (from > 0) {
                    return " OFFSET ? ROWS ";
                } else {
                    return "";
                }
            default: {
                if (limit > 0 && from > 0) {
                    return " LIMIT ? OFFSET ? ";
                } else if (limit > 0) {
                    return " LIMIT ? ";
                } else if (from > 0) {
                    return " OFFSET ? ";
                } else {
                    return "";
                }
            }
        }


    }

    public static int setLimits(int index, PreparedStatement pstmt, int limit) throws SQLException {
//        logger.debug("Limits: "+index+" "+limit);
//        logger.debug(pstmt.toString());
        pstmt.setInt(index++, limit);
        return index;
    }

    public static int setLimits(int index, PreparedStatement pstmt, int from, int to) throws SQLException {
        int limit = to >= 0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;

        switch (Db.getDatabaseType()) {
            case FIREBIRD: {
                if (limit > 0 && from > 0) {
                    pstmt.setInt(index++, limit);
                    pstmt.setInt(index++, from + limit);
                }
                else if (from > 0) {
                    pstmt.setInt(index++, from);
                    // emulate offset function without knowing the amount of rows available
                    // by using the biggest bigint
                    pstmt.setLong(index++, 9223372036854775807L);
                }
                else if (limit > 0) {
                    pstmt.setInt(index++, limit);
                }
                break;
            }
            default: {
                if (limit > 0) {
                    pstmt.setInt(index++, limit);
                }
                if (from > 0) {
                    pstmt.setInt(index++, from);
                }
            }
        }

        return index;
    }

}
