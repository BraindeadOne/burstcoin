package nxt.db.sql;

import nxt.db.PeerDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class SqlPeerDb implements PeerDb {

     @Override public List<String> loadPeers() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM peer")) {
            List<String> peers = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    peers.add(rs.getString("address"));
                }
            }
            return peers;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override public void deletePeers(Collection<String> peers) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM peer WHERE address = ?")) {
            for (String peer : peers) {
                pstmt.setString(1, peer);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override public void addPeers(Collection<String> peers) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("INSERT INTO peer (address) values (?)")) {
            for (String peer : peers) {
                pstmt.setString(1, peer);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
