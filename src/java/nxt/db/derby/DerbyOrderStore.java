package nxt.db.derby;

import nxt.Nxt;
import nxt.Order;
import nxt.db.sql.SqlOrderStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class DerbyOrderStore extends SqlOrderStore {
    @Override
    protected void saveOrder(Connection con, String table, Order order) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, account_id, asset_id, "
                + "price, quantity, creation_height, height, latest) KEY (id, height)  VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, order.getId());
            pstmt.setLong(++i, order.getAccountId());
            pstmt.setLong(++i, order.getAssetId());
            pstmt.setLong(++i, order.getPriceNQT());
            pstmt.setLong(++i, order.getQuantityQNT());
            pstmt.setInt(++i, order.getHeight());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }
}
