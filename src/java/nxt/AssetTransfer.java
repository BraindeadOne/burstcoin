package nxt;

import nxt.db.EntityTable;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.util.Listener;
import nxt.util.Listeners;

public class AssetTransfer {

    public static enum Event {
        ASSET_TRANSFER
    }

    private static final Listeners<AssetTransfer, Event> listeners = new Listeners<>();

    private static final NxtKey.LongKeyFactory<AssetTransfer> transferDbKeyFactory = Nxt.getStores().getAssetTransferStore().getTransferDbKeyFactory();

    private static final EntityTable<AssetTransfer> assetTransferTable = Nxt.getStores().getAssetTransferStore().getAssetTransferTable();

    public static NxtIterator<AssetTransfer> getAllTransfers(int from, int to) {
        return assetTransferTable.getAll(from, to);
    }

    public static int getCount() {
        return assetTransferTable.getCount();
    }

    public static boolean addListener(Listener<AssetTransfer> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<AssetTransfer> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static NxtIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
        return Nxt.getStores().getAssetTransferStore().getAssetTransfers(assetId, from, to);
    }

    public static NxtIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
        return Nxt.getStores().getAssetTransferStore().getAccountAssetTransfers(accountId, from, to);
    }

    public static NxtIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
        return Nxt.getStores().getAssetTransferStore().getAccountAssetTransfers(accountId, assetId, from, to);
    }

    public static int getTransferCount(long assetId) {
        return Nxt.getStores().getAssetTransferStore().getTransferCount(assetId);
    }

    static AssetTransfer addAssetTransfer(Transaction transaction, Attachment.ColoredCoinsAssetTransfer attachment) {
        AssetTransfer assetTransfer = new AssetTransfer(transaction, attachment);
        assetTransferTable.insert(assetTransfer);
        listeners.notify(assetTransfer, Event.ASSET_TRANSFER);
        return assetTransfer;
    }

    static void init() {
    }


    private final long id;
    public final NxtKey dbKey;
    private final long assetId;
    private final int height;
    private final long senderId;
    private final long recipientId;
    private final long quantityQNT;
    private final int timestamp;

    private AssetTransfer(Transaction transaction, Attachment.ColoredCoinsAssetTransfer attachment) {
        this.id = transaction.getId();
        this.dbKey = transferDbKeyFactory.newKey(this.id);
        this.height = transaction.getHeight();
        this.assetId = attachment.getAssetId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.quantityQNT = attachment.getQuantityQNT();
        this.timestamp = transaction.getBlockTimestamp();
    }

    protected AssetTransfer(long id, NxtKey dbKey, long assetId, int height, long senderId, long recipientId, long quantityQNT, int timestamp) {
        this.id = id;
        this.dbKey = dbKey;
        this.assetId = assetId;
        this.height = height;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.quantityQNT = quantityQNT;
        this.timestamp = timestamp;
    }


    public long getId() {
        return id;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getHeight() {
        return height;
    }

}
