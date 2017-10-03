package nxt.db.derby;

import nxt.db.sql.Db;
import nxt.db.store.*;

public class DerbyStores implements Stores {
    private final AccountStore accountStore;
    private final AliasStore aliasStore;
    private final DerbyAssetTransferStore assetTransferStore;
    private final AssetStore assetStore;
    private final ATStore atStore;
    private final BlockchainStore blockchainStore;
    private final DigitalGoodsStoreStore digitalGoodsStoreStore;
    private final EscrowStore escrowStore;
    private final OrderStore orderStore;
    private final PollStore pollStore;
    private final TradeStore tradeStore;
    private final VoteStore voteStore;
    private final TransactionProcessorStore transactionProcessorStore;
    private final SubscriptionStore subscriptionStore;

    public DerbyStores() {

        this.accountStore = new DerbyAccountStore();
        this.aliasStore = new DerbyAliasStore();
        this.assetStore = new DerbyAssetStore();
        this.assetTransferStore = new DerbyAssetTransferStore();
        this.atStore = new DerbyATStore();
        this.blockchainStore = new DerbyBlockchainStore();
        this.digitalGoodsStoreStore = new DerbyDigitalGoodsStoreStore();
        this.escrowStore = new DerbyEscrowStore();
        this.orderStore = new DerbyOrderStore();
        this.pollStore = new DerbyPollStore();
        this.tradeStore = new DerbyTradeStore();
        this.voteStore = new DerbyVoteStore();
        this.transactionProcessorStore = new DerbyTransactionProcessorStore();
        this.subscriptionStore = new DerbySubscriptionStore();
    }

    @Override
    public AccountStore getAccountStore() {
        return accountStore;
    }

    @Override
    public AliasStore getAliasStore() {
        return aliasStore;
    }

    @Override
    public AssetStore getAssetStore() {
        return assetStore;
    }

    @Override
    public DerbyAssetTransferStore getAssetTransferStore() {
        return assetTransferStore;
    }

    @Override
    public ATStore getAtStore() {
        return atStore;
    }

    @Override
    public BlockchainStore getBlockchainStore() {
        return blockchainStore;
    }

    @Override
    public DigitalGoodsStoreStore getDigitalGoodsStoreStore() {
        return digitalGoodsStoreStore;
    }

    @Override
    public void beginTransaction() {
        Db.beginTransaction();
    }

    @Override
    public void commitTransaction() {
        Db.commitTransaction();
    }

    @Override
    public void rollbackTransaction() {
        Db.rollbackTransaction();
    }

    @Override
    public void endTransaction() {
        Db.endTransaction();
    }

    @Override
    public boolean isInTransaction() {
        return Db.isInTransaction();
    }

    @Override
    public EscrowStore getEscrowStore() {
        return escrowStore;
    }

    @Override
    public OrderStore getOrderStore() {
        return orderStore;
    }

    @Override
    public PollStore getPollStore() {
        return pollStore;
    }

    @Override
    public TradeStore getTradeStore() {
        return tradeStore;
    }

    @Override
    public VoteStore getVoteStore() {
        return voteStore;
    }

    @Override
    public TransactionProcessorStore getTransactionProcessorStore() {
        return transactionProcessorStore;
    }

    @Override
    public SubscriptionStore getSubscriptionStore() {
        return subscriptionStore;
    }
}
