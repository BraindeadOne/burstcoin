package nxt.at;

import nxt.Appendix;
import nxt.Constants;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;

import java.util.Arrays;

//NXT API IMPLEMENTATION

public class AT_API_Platform_Impl extends AT_API_Impl {

	private static final Logger logger = LoggerFactory.getLogger(AT_API_Platform_Impl.class);

	private final static AT_API_Platform_Impl instance = new AT_API_Platform_Impl();


	AT_API_Platform_Impl()
	{

	}

	public static AT_API_Platform_Impl getInstance()
	{
		return instance;
	}

	@Override
	public long get_Block_Timestamp( AT_Machine_State state )
	{

		int height = state.getHeight();
		long res =AT_API_Helper.getLongTimestamp( height , 0 );
		if (logger.isTraceEnabled())
		{
			long id = AT_API_Helper.getLong(state.getId());
			logger.trace(String.format("get_Block_Timestamp: state: %d height: %d ts: %d", id, height, res));
		}
		return res;

	}

	public long get_Creation_Timestamp( AT_Machine_State state )
	{
		long res =  AT_API_Helper.getLongTimestamp( state.getCreationBlockHeight() , 0 );
        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("get_Creation_Timestamp: state: %d height: %d ts: %d", id, state.getCreationBlockHeight(), res));
        }
		return res;
	}

	@Override
	public long get_Last_Block_Timestamp( AT_Machine_State state )
	{

		int height = state.getHeight() - 1;
		long res =  AT_API_Helper.getLongTimestamp( height , 0 );
        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("get_Last_Block_Timestamp: state: %d height: %d ts: %d", id, height, res));
        }
		return res;
	}

	@Override
	public void put_Last_Block_Hash_In_A( AT_Machine_State state ) {
		ByteBuffer b = ByteBuffer.allocate( state.get_A1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( Nxt.getBlockchain().getBlockAtHeight(state.getHeight() - 1).getBlockHash() );

		b.clear();

		byte[] temp = new byte[ 8 ];

		b.get( temp, 0 , 8 );
		state.set_A1( temp );

		b.get( temp , 0 , 8 );
		state.set_A2( temp );

		b.get( temp , 0 , 8 );
		state.set_A3( temp );

		b.get( temp , 0 , 8 );
		state.set_A4( temp );

        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("put_Last_Block_Hash_In_A: state: %d A1: %s A2: %s A3: %s A4: %s", id,
                    Convert.toHexString(state.get_A1()),
                    Convert.toHexString(state.get_A2()),
                    Convert.toHexString(state.get_A3()),
                    Convert.toHexString(state.get_A4())
                    ));
        }

	}

	@Override
	public void A_to_Tx_after_Timestamp( long val , AT_Machine_State state ) {

		int height = AT_API_Helper.longToHeight( val );
		int numOfTx = AT_API_Helper.longToNumOfTx( val );

		byte[] b = state.getId();

		long tx = findTransaction( height , state.getHeight() , AT_API_Helper.getLong( b ) , numOfTx , state.minActivationAmount() );
		logger.trace("A_to_Tx_after_Timestamp: tx with id "+tx+" found");
		clear_A( state );
		state.set_A1( AT_API_Helper.getByteArray( tx ) );
        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("A_to_Tx_after_Timestamp: state: %d A1: %s ", id,
                    Convert.toHexString(state.get_A1())
            ));
        }
	}

	@Override
	public long get_Type_for_Tx_in_A( AT_Machine_State state ) {
		long txid = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txid );

		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}

		if ( tx != null )
		{
			if (tx.getMessage() != null )
			{
                logger.trace("get_Type_for_Tx_in_A: 1");
				return 1;
			}
			else
			{
                logger.trace("get_Type_for_Tx_in_A: 0");
				return 0;
			}
		}
        logger.trace("get_Type_for_Tx_in_A: -1");
		return -1;
	}

	@Override
	public long get_Amount_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txId );

		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}

		long amount = -1;
		if ( tx != null )
		{
			if( (tx.getMessage() == null || state.getHeight() >= Constants.AT_FIX_BLOCK_2) && state.minActivationAmount() <= tx.getAmountNQT() )
			{
				amount = tx.getAmountNQT() - state.minActivationAmount();
			}
			else
			{
				amount = 0;
			}
		}
        logger.trace("get_Amount_for_Tx_in_A: "+amount);
		return amount;
	}

	@Override
	public long get_Timestamp_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );
		logger.trace("get_Timestamp_for_Tx_in_A: get timestamp for tx with id " + txId + " found");
		Transaction tx = Nxt.getBlockchain().getTransaction( txId );

		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}

		if ( tx != null )
		{
			int blockHeight = tx.getHeight();

			byte[] b = state.getId();

			int txHeight = findTransactionHeight( txId , blockHeight , AT_API_Helper.getLong( b ) , state.minActivationAmount() );

			long res =  AT_API_Helper.getLongTimestamp( blockHeight , txHeight );

            if (logger.isTraceEnabled())
            {
                long id = AT_API_Helper.getLong(state.getId());
                logger.trace(String.format("get_Timestamp_for_Tx_in_A: state: %d ts: %s ", id,
                        res
                ));
            }

			return res;
		}
		return -1;
	}

	@Override
	public long get_Random_Id_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txId );

		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}

		if ( tx !=null )
		{
			int txBlockHeight = tx.getHeight();


			int blockHeight = state.getHeight();

			if ( blockHeight - txBlockHeight < AT_Constants.getInstance().BLOCKS_FOR_RANDOM( blockHeight ) ){ //for tests - for real case 1440
				state.setWaitForNumberOfBlocks( (int)AT_Constants.getInstance().BLOCKS_FOR_RANDOM( blockHeight ) - ( blockHeight - txBlockHeight ) );
				state.getMachineState().pc -= 7;
				state.getMachineState().stopped = true;
				if (logger.isTraceEnabled())
				{
					long id = AT_API_Helper.getLong(state.getId());
					logger.trace(String.format("get_Random_Id_for_Tx_in_A: state: %d id: %s ", id, 0));
				}
				return 0;
			}

			MessageDigest digest = Crypto.sha256();

			byte[] senderPublicKey = tx.getSenderPublicKey();

			ByteBuffer bf = ByteBuffer.allocate( 32 + Long.SIZE + senderPublicKey.length );
			bf.order( ByteOrder.LITTLE_ENDIAN );
			bf.put(Nxt.getBlockchain().getBlockAtHeight(blockHeight - 1).getGenerationSignature());
			bf.putLong( tx.getId() );
			bf.put( senderPublicKey);

			digest.update(bf.array());
			byte[] byteRandom = digest.digest();

			long random = Math.abs( AT_API_Helper.getLong( Arrays.copyOfRange(byteRandom, 0, 8) ) );

			if (logger.isTraceEnabled())
			{
				long id = AT_API_Helper.getLong(state.getId());
				logger.trace(String.format("get_Random_Id_for_Tx_in_A: state: %d id: %s ", id,
						Convert.toUnsignedLong( tx.getId() )
				));
			}
			//System.out.println( "info: random for txid: " + Convert.toUnsignedLong( tx.getId() ) + "is: " + random );
			return random;
		}
		return -1;
	}

	@Override
	public void message_from_Tx_in_A_to_B( AT_Machine_State state ) {
		long txid = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txid );
		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}
		ByteBuffer b = ByteBuffer.allocate( state.get_A1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );
		if( tx != null )
		{
			Appendix.Message txMessage = tx.getMessage();
			if(txMessage != null)
			{
				byte[] message = txMessage.getMessage();
				if ( message.length <= state.get_A1().length * 4 )
				{
					b.put( message );
				}
			}
		}

		b.clear();

		byte[] temp = new byte[ 8 ];

		b.get( temp, 0 , 8 );
		state.set_B1( temp );

		b.get( temp , 0 , 8 );
		state.set_B2( temp );

		b.get( temp , 0 , 8 );
		state.set_B3( temp );

		b.get( temp , 0 , 8 );
		state.set_B4( temp );

        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("message_from_Tx_in_A_to_B: state: %d B1: %s B2: %s B3: %s B4: %s", id,
                    Convert.toHexString(state.get_B1()),
                    Convert.toHexString(state.get_B2()),
                    Convert.toHexString(state.get_B3()),
                    Convert.toHexString(state.get_B4())
            ));
        }

	}
	@Override
	public void B_to_Address_of_Tx_in_A( AT_Machine_State state ) {

		long txId = AT_API_Helper.getLong( state.get_A1() );

		clear_B( state );

		Transaction tx = Nxt.getBlockchain().getTransaction( txId );
		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}
		if( tx != null )
		{
			long address = tx.getSenderId();
			state.set_B1( AT_API_Helper.getByteArray( address ) );
		}

        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("B_to_Address_of_Tx_in_A: state: %d B1: %s ", id,
                    Convert.toHexString(state.get_B1())
            ));
        }
	}

	@Override
	public void B_to_Address_of_Creator( AT_Machine_State state ) {
		long creator = AT_API_Helper.getLong( state.getCreator() );

		clear_B( state );

		state.set_B1( AT_API_Helper.getByteArray( creator ) );
        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("B_to_Address_of_Creator: state: %d B1: %s ", id,
                    Convert.toHexString(state.get_B1())
                    ));
        }
	}

	@Override
	public void put_Last_Block_Generation_Signature_In_A( AT_Machine_State state ) {
		ByteBuffer b = ByteBuffer.allocate( state.get_A1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( Nxt.getBlockchain().getBlockAtHeight(state.getHeight() - 1).getGenerationSignature() );

		byte[] temp = new byte[ 8 ];

		b.get( temp, 0 , 8 );
		state.set_A1( temp );

		b.get( temp , 0 , 8 );
		state.set_A2( temp );

		b.get( temp , 0 , 8 );
		state.set_A3( temp );

		b.get( temp , 0 , 8 );
		state.set_A4( temp );

        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("put_Last_Block_Generation_Signature_In_A: state: %d A1: %s A2: %s A3: %s A4: %s", id,
                    Convert.toHexString(state.get_A1()),
                    Convert.toHexString(state.get_A2()),
                    Convert.toHexString(state.get_A3()),
                    Convert.toHexString(state.get_A4())
            ));
        }

	}

	@Override
	public long get_Current_Balance( AT_Machine_State state ) {
		if(state.getHeight() < Constants.AT_FIX_BLOCK_2 )
			return 0;

		//long balance = Account.getAccount( AT_API_Helper.getLong(state.getId()) ).getBalanceNQT();
		Long res =state.getG_balance();
        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("get_Current_Balance: state: %d balance: %d ", id, res));
        }
        return res;
	}

	@Override
	public long get_Previous_Balance( AT_Machine_State state ) {
		if(state.getHeight() < Constants.AT_FIX_BLOCK_2 )
			return 0;
		Long res = state.getP_balance();

        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("get_Previous_Balance: state: %d balance: %d ", id, res));
        }
		return res;
	}

	@Override
	public void send_to_Address_in_B( long val , AT_Machine_State state ) {
		/*ByteBuffer b = ByteBuffer.allocate( state.get_B1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( state.get_B1() );
		b.put( state.get_B2() );
		b.put( state.get_B3() );
		b.put( state.get_B4() );
		*/

		if ( val < 1 ) return;

		if ( val < state.getG_balance() )
		{

			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , val , null );
			state.addTransaction( tx );
			state.setG_balance( state.getG_balance() - val );
		}
		else
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , state.getG_balance() , null );
			state.addTransaction( tx );
			state.setG_balance( 0L );
		}
        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("send_to_Address_in_B: state: %d balance: %d val: %d", id, state.getG_balance(), val));
        }

	}

	@Override
	public void send_All_to_Address_in_B( AT_Machine_State state ) {
		/*ByteBuffer b = ByteBuffer.allocate( state.get_B1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( state.get_B1() );
		b.put( state.get_B2() );
		b.put( state.get_B3() );
		b.put( state.get_B4() );
		 */
		/*byte[] bId = state.getId();
		byte[] temp = new byte[ 8 ];
		for ( int i = 0; i < 8; i++ )
		{
			temp[ i ] = bId[ i ];
		}*/

		AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , state.getG_balance() , null );
		state.addTransaction( tx );
		state.setG_balance( 0L );
        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("send_All_to_Address_in_B: state: %d balance: %d ", id, state.getG_balance()));
        }
	}

	@Override
	public void send_Old_to_Address_in_B( AT_Machine_State state ) {

		if ( state.getP_balance() > state.getG_balance()  )
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1() , state.getG_balance() , null );
			state.addTransaction( tx );

			state.setG_balance( 0L );
			state.setP_balance( 0L );

		}
		else
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1() , state.getP_balance() , null );
			state.addTransaction( tx );

			state.setG_balance( state.getG_balance() - state.getP_balance() );
			state.setP_balance( 0l );
		}

        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("send_Old_to_Address_in_B: state: %d g_balance: %d b_balance: %d", id, state.getG_balance(), state.getP_balance()));
        }

	}

	@Override
	public void send_A_to_Address_in_B( AT_Machine_State state ) {

		ByteBuffer b = ByteBuffer.allocate(32);
		b.order( ByteOrder.LITTLE_ENDIAN );
		b.put(state.get_A1());
		b.put(state.get_A2());
		b.put(state.get_A3());
		b.put(state.get_A4());
		b.clear();

		AT_Transaction tx = new AT_Transaction( state.getId(), state.get_B1(), 0L, b.array() );
		state.addTransaction(tx);
        if (logger.isTraceEnabled())
        {
            long id = AT_API_Helper.getLong(state.getId());
            logger.trace(String.format("send_A_to_Address_in_B: state: %d ", id));
        }

	}

	public long add_Minutes_to_Timestamp( long val1 , long val2 , AT_Machine_State state) {
		int height = AT_API_Helper.longToHeight( val1 );
		int numOfTx = AT_API_Helper.longToNumOfTx( val1 );
		int addHeight = height + (int) (val2 / AT_Constants.getInstance().AVERAGE_BLOCK_MINUTES( state.getHeight() ));

		return AT_API_Helper.getLongTimestamp( addHeight , numOfTx );
	}

	protected static Long findTransaction(int startHeight , int endHeight , Long atID, int numOfTx, long minAmount){
		return Nxt.getStores().getAtStore().findTransaction(startHeight, endHeight, atID, numOfTx, minAmount);
	}

	protected static int findTransactionHeight(Long transactionId, int height, Long atID, long minAmount){
		return Nxt.getStores().getAtStore().findTransactionHeight(transactionId, height,atID, minAmount);
	}


}
