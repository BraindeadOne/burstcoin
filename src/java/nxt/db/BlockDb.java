package nxt.db;

import nxt.BlockImpl;
import nxt.NxtException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

public interface BlockDb {
    BlockImpl findBlock(long blockId);

    boolean hasBlock(long blockId);

    long findBlockIdAtHeight(int height);

    BlockImpl findBlockAtHeight(int height);

    BlockImpl findLastBlock();

    BlockImpl findLastBlock(int timestamp);

    BlockImpl loadBlock(Connection con, ResultSet rs) throws NxtException.ValidationException;



    void saveBlock(Connection con, BlockImpl block);

    // relying on cascade triggers in the database to delete the transactions for all deleted blocks
    void deleteBlocksFrom(long blockId);

    void deleteAll();

    List<Long> getBlockIdsFromHeight(int height);
    BlockImpl loadBlock(Long id) throws NxtException.ValidationException;

}
