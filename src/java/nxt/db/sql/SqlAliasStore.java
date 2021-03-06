package nxt.db.sql;

import nxt.Alias;
import nxt.Nxt;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;
import nxt.db.store.AliasStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SqlAliasStore implements AliasStore {


    private static final DbKey.LongKeyFactory<Alias.Offer> offerDbKeyFactory = new DbKey.LongKeyFactory<Alias.Offer>("id") {
        @Override
        public NxtKey newKey(Alias.Offer offer) {
            return offer.dbKey;
        }
    };

    @Override
    public NxtKey.LongKeyFactory<Alias.Offer> getOfferDbKeyFactory() {
        return offerDbKeyFactory;
    }

    private static final NxtKey.LongKeyFactory<Alias> aliasDbKeyFactory = new DbKey.LongKeyFactory<Alias>("id") {

        @Override
        public NxtKey newKey(Alias alias) {
            return alias.dbKey;
        }
    };

    @Override
    public NxtKey.LongKeyFactory<Alias> getAliasDbKeyFactory() {
        return aliasDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<Alias> getAliasTable() {
        return aliasTable;
    }

    private class SqlOffer extends Alias.Offer {

        private SqlOffer(ResultSet rs) throws SQLException {
            super(rs.getLong("id"), rs.getLong("price"), rs.getLong("buyer_id"), offerDbKeyFactory.newKey(rs.getLong("id")));
        }


    }

    protected void saveOffer(Alias.Offer offer, Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias_offer (id, price, buyer_id, "
                + "height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, offer.getId());
            pstmt.setLong(++i, offer.getPriceNQT());
            DbUtils.setLongZeroToNull(pstmt, ++i, offer.getBuyerId());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    private final VersionedEntityTable<Alias.Offer> offerTable = new VersionedEntitySqlTable<Alias.Offer>("alias_offer", offerDbKeyFactory) {

        @Override
        protected Alias.Offer load(Connection con, ResultSet rs) throws SQLException {
            return new SqlOffer(rs);
        }

        @Override
        protected void save(Connection con, Alias.Offer offer) throws SQLException {
            saveOffer(offer, con);
        }
    };

    @Override
    public VersionedEntityTable<Alias.Offer> getOfferTable() {
        return offerTable;
    }

    private class SqlAlias extends Alias {
        private SqlAlias(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("id"),
                    rs.getLong("account_id"),
                    rs.getString("alias_name"),
                    rs.getString("alias_uri"),
                    rs.getInt("timestamp"),
                    aliasDbKeyFactory.newKey(rs.getLong("id"))
            );
        }


    }

    protected void saveAlias(Alias alias, Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias (id, account_id, alias_name, "
                + "alias_uri, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, alias.getId());
            pstmt.setLong(++i, alias.getAccountId());
            pstmt.setString(++i, alias.getAliasName());
            pstmt.setString(++i, alias.getAliasURI());
            pstmt.setInt(++i, alias.getTimestamp());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    private final VersionedEntityTable<Alias> aliasTable = new VersionedEntitySqlTable<Alias>("alias", aliasDbKeyFactory) {

        @Override
        protected Alias load(Connection con, ResultSet rs) throws SQLException {
            return new SqlAlias(rs);
        }

        @Override
        protected void save(Connection con, Alias alias) throws SQLException {
            saveAlias(alias, con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY alias_name_lower ";
        }

    };

    @Override
    public NxtIterator<Alias> getAliasesByOwner(long accountId, int from, int to) {
        return aliasTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    @Override
    public Alias getAlias(String aliasName) {
        return aliasTable.getBy(new DbClause.StringClause("alias_name_lower", aliasName.toLowerCase()));
    }

}
