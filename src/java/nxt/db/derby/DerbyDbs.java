package nxt.db.derby;

import nxt.TransactionDb;
import nxt.db.BlockDb;
import nxt.db.PeerDb;
import nxt.db.store.Dbs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Db-Classes are necessary for the instanciation of some stores and have to be handled separately. In the original version these were static
 */
public class DerbyDbs implements Dbs {

    private final BlockDb blockDb;
    private final TransactionDb transactionDb;
    private final PeerDb peerDb;


    public DerbyDbs() {
        DerbyDbVersion.init();
        this.blockDb = new DerbyBlockDB();
        this.transactionDb = new DerbyTransactionDb();
        this.peerDb = new DerbyPeerDb();
    }

    @Override
    public BlockDb getBlockDb() {
        return blockDb;
    }

    @Override
    public TransactionDb getTransactionDb() {
        return transactionDb;
    }

    @Override
    public PeerDb getPeerDb() {
        return peerDb;
    }

    @Override
    public void disableForeignKeyChecks(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
//        stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
    }

    @Override
    public void enableForeignKeyChecks(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
//        stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
