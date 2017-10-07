/*
 * Some portion .. Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.

 */

package nxt;


import nxt.at.AT_API_Helper;
import nxt.at.AT_Controller;
import nxt.at.AT_Machine_State;
import nxt.at.AT_Transaction;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;
import nxt.util.Convert;
import nxt.util.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.SubstituteLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class AT extends AT_Machine_State {
	private static final Logger logger = LoggerFactory.getLogger(AT_Machine_State.class);

	static {
		Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
			@Override
			public void notify(Block block) {
				for(Long id : pendingFees.keySet()) {
					Account atAccount = Account.getAccount(id);
					atAccount.addToBalanceAndUnconfirmedBalanceNQT(-pendingFees.get(id));
				}
				List<TransactionImpl> transactions = new ArrayList<>();
				for(AT_Transaction atTransaction : pendingTransactions) {
					Account.getAccount(AT_API_Helper.getLong(atTransaction.getSenderId())).addToBalanceAndUnconfirmedBalanceNQT(-atTransaction.getAmount());
					Account.addOrGetAccount(AT_API_Helper.getLong(atTransaction.getRecipientId())).addToBalanceAndUnconfirmedBalanceNQT(atTransaction.getAmount());

					TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl((byte)1, Genesis.CREATOR_PUBLIC_KEY,
							atTransaction.getAmount(), 0L, block.getTimestamp(), (short)1440, Attachment.AT_PAYMENT);

					builder.senderId(AT_API_Helper.getLong(atTransaction.getSenderId()))
						.recipientId(AT_API_Helper.getLong(atTransaction.getRecipientId()))
						.blockId(block.getId())
						.height(block.getHeight())
						.blockTimestamp(block.getTimestamp())
						.ecBlockHeight(0)
						.ecBlockId(0L);

					byte[] message = atTransaction.getMessage();
					if(message != null) {
						builder.message(new Appendix.Message(message));
					}

					try {
						TransactionImpl transaction = builder.build();
						if(!Nxt.getDbs().getTransactionDb().hasTransaction(transaction.getId())) {
							transactions.add(transaction);
						}
					}
					catch(NxtException.NotValidException e) {
						throw new RuntimeException("Failed to construct AT payment transaction", e);
					}
				}

				if(transactions.size() > 0) {
					/** WATCH: Replace after transactions are converted! */
					Nxt.getDbs().getTransactionDb().saveTransactions( transactions);
				}

			}

		}, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
	}

	private static final LinkedHashMap<Long, Long> pendingFees = new LinkedHashMap<>();
	private static final List<AT_Transaction> pendingTransactions = new ArrayList<>();

	public static void clearPendingFees() {
		pendingFees.clear();
	}

	public static void clearPendingTransactions() {
		pendingTransactions.clear();
	}

	public static void addPendingFee(long id, long fee) {
		pendingFees.put(id, fee);
	}

	public static void addPendingFee(byte[] id, long fee) {
		addPendingFee(AT_API_Helper.getLong(id), fee);
	}

	public static void addPendingTransaction(AT_Transaction atTransaction) {
		pendingTransactions.add(atTransaction);
	}

	public static boolean findPendingTransaction(byte[] recipientId) {
		for(AT_Transaction tx : pendingTransactions) {
			if(Arrays.equals(recipientId, tx.getRecipientId())) {
				return true;
			}
		}
		return false;
	}

	public static class ATState {

		private final long atId;
		public final NxtKey dbKey;

		private byte[] state;
		private int prevHeight;
		private int nextHeight;
		private int sleepBetween;
		private long prevBalance;
		private boolean freezeWhenSameBalance;
		private long minActivationAmount;

		protected ATState(long atId, byte[] state, int prevHeight,
						  int nextHeight, int sleepBetween, long prevBalance, boolean freezeWhenSameBalance, long minActivationAmount) {
			this.atId = atId;
			this.dbKey =  atStateDbKeyFactory.newKey(this.atId);
			this.state = state;
			this.nextHeight = nextHeight;
			this.sleepBetween = sleepBetween;
			this.prevBalance = prevBalance;
			this.freezeWhenSameBalance = freezeWhenSameBalance;
			this.minActivationAmount = minActivationAmount;
			if (logger.isTraceEnabled())
				logger.trace("AT_Machine_State: "+this.toString());
		}



		public long getATId() {
			return atId;
		}

		public byte[] getState() {
			return state;
		}

		public int getPrevHeight() {
			return prevHeight;
		}

		public int getNextHeight() {
			return nextHeight;
		}

		public int getSleepBetween() {
			return sleepBetween;
		}

		public long getPrevBalance() {
			return prevBalance;
		}

		public boolean getFreezeWhenSameBalance() {
			return freezeWhenSameBalance;
		}

		public long getMinActivationAmount() {
			return minActivationAmount;
		}

		public void setState(byte[] newState) {
			state = newState;
		}

		public void setPrevHeight(int prevHeight){
			this.prevHeight = prevHeight;
		}

		public void setNextHeight(int newNextHeight) {
			nextHeight = newNextHeight;
		}

		public void setSleepBetween(int newSleepBetween) {
			this.sleepBetween = newSleepBetween;
		}

		public void setPrevBalance(long newPrevBalance) {
			this.prevBalance = newPrevBalance;
		}

		public void setFreezeWhenSameBalance(boolean newFreezeWhenSameBalance) {
			this.freezeWhenSameBalance = newFreezeWhenSameBalance;
		}

		public void setMinActivationAmount(long newMinActivationAmount) {
			this.minActivationAmount = newMinActivationAmount;
		}

		@Override
		public String toString() {
			return "ATState{" +
					"atId=" + atId +
					", dbKey=" + dbKey +
					", state=" + Convert.toHexString(state) +
					", prevHeight=" + prevHeight +
					", nextHeight=" + nextHeight +
					", sleepBetween=" + sleepBetween +
					", prevBalance=" + prevBalance +
					", freezeWhenSameBalance=" + freezeWhenSameBalance +
					", minActivationAmount=" + minActivationAmount +
					'}';
		}
	}

	private static final NxtKey.LongKeyFactory<AT> atDbKeyFactory =Nxt.getStores().getAtStore().getAtDbKeyFactory();

	private static final VersionedEntityTable<AT> atTable = Nxt.getStores().getAtStore().getAtTable();


	private static final NxtKey.LongKeyFactory<ATState> atStateDbKeyFactory = Nxt.getStores().getAtStore().getAtStateDbKeyFactory();

	private static final VersionedEntityTable<ATState> atStateTable = Nxt.getStores().getAtStore().getAtStateTable();

	public static Collection<Long> getAllATIds()
	{
		return Nxt.getStores().getAtStore().getAllATIds();
	}

	public static AT getAT(byte[] id)
	{
		return getAT( AT_API_Helper.getLong( id ) );
	}

	public static AT getAT(Long id) {
		return Nxt.getStores().getAtStore().getAT(id);
	}

	public static List<Long> getATsIssuedBy(Long accountId) {
		return Nxt.getStores().getAtStore().getATsIssuedBy(accountId);
	}

	static void addAT(Long atId, Long senderAccountId, String name, String description, byte[] creationBytes , int height) {

		ByteBuffer bf = ByteBuffer.allocate( 8 + 8 );
		bf.order( ByteOrder.LITTLE_ENDIAN );

		bf.putLong( atId );

		byte[] id = new byte[ 8 ];

		bf.putLong( 8 , senderAccountId );

		byte[] creator = new byte[ 8 ];
		bf.clear();
		bf.get( id , 0 , 8 );
		bf.get( creator , 0 , 8);

		AT at = new AT( id , creator , name , description , creationBytes , height );

		AT_Controller.resetMachine(at);

		atTable.insert(at);

		at.saveState();

		Account account = Account.addOrGetAccount(atId);
		account.apply(new byte[32], height);
	}

	public void saveState() {
		ATState state = atStateTable.get(atStateDbKeyFactory.newKey( AT_API_Helper.getLong( this.getId() ) ) );
		int prevHeight = Nxt.getBlockchain().getHeight();
		int nextHeight = prevHeight + getWaitForNumberOfBlocks();
		if(state != null) {
			state.setState(getState());
			state.setPrevHeight( prevHeight );
			state.setNextHeight(nextHeight);
			state.setSleepBetween(getSleepBetween());
			state.setPrevBalance(getP_balance());
			state.setFreezeWhenSameBalance(freezeOnSameBalance());
			state.setMinActivationAmount(minActivationAmount());
		}
		else {
			state = new ATState( AT_API_Helper.getLong( this.getId() ) , getState(), prevHeight, nextHeight, getSleepBetween(), getP_balance(), freezeOnSameBalance(), minActivationAmount());
		}
		atStateTable.insert(state);
		if (logger.isTraceEnabled())
			logger.trace("saveState:"+state.toString());
	}


	private static void deleteAT( AT at )
	{
		ATState atState = atStateTable.get(atStateDbKeyFactory.newKey(AT_API_Helper.getLong(at.getId())));
		if(atState != null) {
			atStateTable.delete(atState);
		}
		atTable.delete(at);
		//TODO: release account
	}

	private static void deleteAT( Long id )
	{
		AT at = AT.getAT(id);
		if(at != null) {
			deleteAT(at);
		}

	}

	public static List< Long > getOrderedATs(){
		return Nxt.getStores().getAtStore().getOrderedATs();
	}


	static boolean isATAccountId(Long id) {
		return Nxt.getStores().getAtStore().isATAccountId(id);
	}

	public static byte[] compressState(byte[] stateBytes) {
		if(stateBytes == null || stateBytes.length == 0) {
			return null;
		}

		try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
			gzip.write(stateBytes);
			gzip.flush();
			gzip.close();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static byte[] decompressState(byte[] stateBytes) {
		if(stateBytes == null || stateBytes.length == 0) {
			return null;
		}

		try(ByteArrayInputStream bis = new ByteArrayInputStream(stateBytes);
				GZIPInputStream gzip = new GZIPInputStream(bis);
				ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[256];
			int read;
			while((read = gzip.read(buffer, 0, buffer.length)) > 0) {
				bos.write(buffer, 0, read);
			}
			bos.flush();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	static void init() {}

	private final String name;
	private final String description;
	public final NxtKey dbKey;
	private final int nextHeight;


	private AT( byte[] atId , byte[] creator , String name , String description , byte[] creationBytes , int height ) {
		super( atId , creator , creationBytes , height );
		this.name = name;
		this.description = description;
		dbKey = atDbKeyFactory.newKey(AT_API_Helper.getLong(atId));
		this.nextHeight = Nxt.getBlockchain().getHeight();
	}

	public AT ( byte[] atId , byte[] creator , String name , String description , short version ,
			byte[] stateBytes, int csize , int dsize , int c_user_stack_bytes , int c_call_stack_bytes ,
			int creationBlockHeight, int sleepBetween , int nextHeight ,
			boolean freezeWhenSameBalance, long minActivationAmount, byte[] apCode )
	{
		super( 	atId , creator , version ,
				stateBytes , csize , dsize , c_user_stack_bytes , c_call_stack_bytes ,
				creationBlockHeight , sleepBetween ,
				freezeWhenSameBalance , minActivationAmount , apCode );
		this.name = name;
		this.description = description;
		dbKey = atDbKeyFactory.newKey(AT_API_Helper.getLong(atId));
		this.nextHeight = nextHeight;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int nextHeight() {
		return nextHeight;
	}

	public byte[] getApCode() {
		return getAp_code().array();
	}

	public byte[] getApData() {
		return getAp_data().array();
	}

}
