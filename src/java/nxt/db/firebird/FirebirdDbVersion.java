package nxt.db.firebird;

import nxt.db.sql.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class FirebirdDbVersion {

    private static final Logger logger = LoggerFactory.getLogger(FirebirdDbVersion.class);

    static void init() {
        try (Connection con = Db.beginTransaction(); Statement stmt = con.createStatement()) {
            int nextUpdate = 1;
            try {
                ResultSet rs = stmt.executeQuery("SELECT next_update FROM version");
                if (! rs.next()) {
                    throw new RuntimeException("Invalid version table");
                }
                nextUpdate = rs.getInt("next_update");
                if (! rs.isLast()) {
                    throw new RuntimeException("Invalid version table");
                }
                rs.close();
                logger.info("Database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
            } catch (SQLException e) {
                logger.info("Initializing an empty database");
                stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
		Db.commitTransaction();
                stmt.executeUpdate("INSERT INTO version VALUES (1)");
                Db.commitTransaction();
            }
            update(nextUpdate);
        } catch (SQLException e) {
            Db.rollbackTransaction();
            throw new RuntimeException(e.toString(), e);
        } finally {
            Db.endTransaction();
        }

    }

    private static void apply(String sql) {
        try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
            try {
                if (sql != null) {
                    logger.debug("Will apply sql:\n" + sql);
                    stmt.executeUpdate(sql);
                }
                stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
                Db.commitTransaction();
            } catch (Exception e) {
                Db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error executing " + sql, e);
        }
    }

    public static String maybeToShortIdentifier( String identifier) {
	return identifier.replaceAll("(.)[^_]+_", "$1" + "_");
    }

    private static void update(int nextUpdate) {
        logger.debug("Next update is "+nextUpdate);
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE alias("
		    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    account_id BIGINT NOT NULL,"
                    + "    alias_name VARCHAR(100) CHARACTER SET UTF8 NOT NULL,"
                    + "    alias_name_LOWER VARCHAR(100) CHARACTER SET UTF8 NOT NULL,"
                    + "    alias_uri BLOB SUB_TYPE TEXT NOT NULL,"
                    + "    \"timestamp\" INT NOT NULL,"
                    + "    height INT NOT NULL,"
		    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(id, height)"
		    + ");");
            case 2:
		apply(
		      "CREATE TRIGGER lower_alias_name FOR ALIAS ACTIVE BEFORE INSERT OR UPDATE POSITION 10 AS BEGIN\n"
		      + "    NEW.alias_name_lower = LOWER(NEW.alias_name);\n"
		      + "END\n;"
		);
            case 3:
                apply("CREATE UNIQUE DESCENDING INDEX alias_id_height_idx ON alias(id, height);");
            case 4:
                apply("CREATE DESCENDING INDEX alias_account_id_idx ON alias(account_id, height);");
            case 5:
                apply("CREATE INDEX alias_name_lower_idx ON alias(alias_name_lower);");
            case 6:
                apply("CREATE TABLE account("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    creation_HEIGHT INT NOT NULL,"
                    + "    public_key CHAR(64),"
                    + "    key_height INT,"
                    + "    balance BIGINT NOT NULL,"
                    + "    unconfirmed_balance BIGINT NOT NULL,"
                    + "    forged_balance BIGINT NOT NULL,"
                    + "    name VARCHAR(100) CHARACTER SET UTF8,"
                    + "    description BLOB SUB_TYPE TEXT,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(id, height)"
                    + ");");
            case 7:
                apply("CREATE UNIQUE DESCENDING INDEX account_id_height_idx ON account(id, height);");
            case 8:
                apply("CREATE DESCENDING INDEX account_id_balance_height_idx ON account(id, balance, height);");
            case 9:
                apply("CREATE TABLE alias_offer("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    price BIGINT NOT NULL,"
                    + "    buyer_ID BIGINT,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
                    + "    UNIQUE(id, height)"
                    + ");");
            case 10:
                apply("CREATE UNIQUE DESCENDING INDEX alias_offer_id_height_idx ON alias_offer(id, height);");
            case 11:
                apply("CREATE TABLE peer("
                    + "    address VARCHAR(100) PRIMARY KEY NOT NULL"
                    + ");");
	    case 12:
                apply("CREATE TABLE transaction("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    deadline SMALLINT NOT NULL,"
                    + "    sender_public_key CHAR(64) NOT NULL,"
                    + "    recipient_id BIGINT,"
                    + "    amount BIGINT NOT NULL,"
                    + "    fee BIGINT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    block_id BIGINT NOT NULL,"
                    + "    signature CHAR(128),"
                    + "    \"timestamp\" INT NOT NULL,"
                    + "    type SMALLINT NOT NULL,"
                    + "    subtype SMALLINT NOT NULL,"
                    + "    sender_id BIGINT NOT NULL,"
                    + "    block_timestamp INT NOT NULL,"
                    + "    full_hash CHAR(64) NOT NULL,"
                    + "    " + maybeToShortIdentifier("referenced_transaction_full_hash") + " CHAR(64),"
                    + "    attachment_bytes BLOB,"
                    + "    version SMALLINT NOT NULL,"
                    + "    has_message BOOLEAN DEFAULT FALSE NOT NULL,"
                    + "    has_encrypted_message BOOLEAN DEFAULT FALSE NOT NULL,"
                    + "    has_public_key_announcement BOOLEAN DEFAULT FALSE NOT NULL,"
                    + "    ec_block_height INT DEFAULT NULL,"
                    + "    ec_block_id BIGINT DEFAULT NULL,"
                    + "    has_encrypttoself_message BOOLEAN DEFAULT FALSE NOT NULL,"
		    + "    UNIQUE(id),"
		    + "    UNIQUE(full_hash)"
                    + ");");
            case 13:
                apply("CREATE DESCENDING INDEX transaction_block_timestamp_idx ON transaction(block_timestamp);");
            case 14:
                apply("CREATE UNIQUE INDEX transaction_id_idx ON transaction(id);");
            case 15:
                apply("CREATE INDEX transaction_sender_id_idx ON transaction(sender_ID);");
            case 16:
                apply("CREATE UNIQUE INDEX transaction_full_hash_idx ON transaction(full_hash);");
            case 17:
                apply("CREATE INDEX transaction_recipient_id_idx ON transaction(recipient_id);");
            case 18:
		apply("CREATE INDEX " + maybeToShortIdentifier("transaction_recipient_id_amount_height_idx") + " ON transaction(recipient_id, amount, height);");
            case 19:
                apply("CREATE TABLE asset("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    account_id BIGINT NOT NULL,"
                    + "    name VARCHAR(10) NOT NULL,"
                    + "    description BLOB SUB_TYPE TEXT,"
                    + "    quantity BIGINT NOT NULL,"
                    + "    decimals SMALLINT NOT NULL,"
                    + "    height INT NOT NULL,"
		    + "    UNIQUE(id)"
                    + ");");
            case 20:
                apply("CREATE UNIQUE INDEX asset_id_idx ON asset(id);");
            case 21:
                apply("CREATE INDEX asset_account_id_idx ON asset(account_id);");
            case 22:
                apply("CREATE TABLE trade("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    asset_id BIGINT NOT NULL,"
                    + "    block_id BIGINT NOT NULL,"
                    + "    ask_order_id BIGINT NOT NULL,"
                    + "    bid_order_id BIGINT NOT NULL,"
                    + "    ask_order_height INT NOT NULL,"
                    + "    bid_order_height INT NOT NULL,"
                    + "    seller_id BIGINT NOT NULL,"
                    + "    buyer_id BIGINT NOT NULL,"
                    + "    quantity BIGINT NOT NULL,"
                    + "    price BIGINT NOT NULL,"
                    + "    \"timestamp\" INT NOT NULL,"
                    + "    height INT NOT NULL,"
		    + "    UNIQUE(ask_order_id, bid_order_id)"
		    + ");");
            case 23:
                apply("CREATE UNIQUE INDEX trade_ask_bid_idx ON trade(ask_order_id, bid_order_id);");
            case 24:
                apply("CREATE DESCENDING INDEX trade_asset_id_idx ON trade(asset_id, height);");
            case 25:
                apply("CREATE DESCENDING INDEX trade_seller_id_idx ON trade(seller_id, height);");
            case 26:
                apply("CREATE DESCENDING INDEX trade_buyer_id_idx ON trade(buyer_id, height);");
            case 27:
                apply("CREATE TABLE ask_order("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id bigint NOT NULL,"
                    + "    account_id BIGINT NOT NULL,"
                    + "    asset_id BIGINT NOT NULL,"
                    + "    price BIGINT NOT NULL,"
                    + "    quantity BIGINT NOT NULL,"
                    + "    creation_height INT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(id, height)"
                    + ");");
            case 28:
                apply("CREATE UNIQUE DESCENDING INDEX ask_order_id_height_idx ON ask_order(id, height);");
            case 29:
                apply("CREATE DESCENDING INDEX ask_order_account_id_idx ON ask_order(account_id, height);");
            case 30:
                apply("CREATE INDEX ask_order_asset_id_price_idx ON ask_order(asset_id, price);");
            case 31:
                apply("CREATE DESCENDING INDEX ask_order_creation_idx ON ask_order(creation_height);");
            case 32:
                apply("CREATE TABLE bid_order("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    account_id BIGINT NOT NULL,"
                    + "    asset_id BIGINT NOT NULL,"
                    + "    price BIGINT NOT NULL,"
                    + "    quantity BIGINT NOT NULL,"
                    + "    creation_height INT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
                    + "    UNIQUE(id, height)"
                    + ");");
            case 33:
                apply("CREATE UNIQUE DESCENDING INDEX bid_order_id_height_idx ON bid_order(id, height);");
            case 34:
                apply("CREATE DESCENDING INDEX bid_order_account_id_idx ON bid_order(account_id, height);");
            case 35:
                apply("CREATE DESCENDING INDEX bid_order_asset_id_price_idx ON bid_order(asset_id, price);");
            case 36:
                apply("CREATE DESCENDING INDEX bid_order_creation_idx ON bid_order(creation_height);");
            case 37:
                apply("CREATE TABLE goods("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    ID BIGINT NOT NULL,"
                    + "    seller_id BIGINT NOT NULL,"
                    + "    name VARCHAR(100) NOT NULL,"
                    + "    description BLOB SUB_TYPE TEXT,"
                    + "    tags VARCHAR(100),"
                    + "    \"timestamp\" INT NOT NULL,"
                    + "    quantity INT NOT NULL,"
                    + "    price BIGINT NOT NULL,"
                    + "    delisted BOOLEAN NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
                    + "    UNIQUE(id, height)"
                    + ");");
            case 38:
                apply("CREATE UNIQUE DESCENDING INDEX goods_id_height_idx ON goods(id, height);");
            case 39:
                apply("CREATE INDEX goods_seller_id_name_idx ON goods(seller_id, NAME);");
            case 40:
                apply("CREATE DESCENDING INDEX goods_timestamp_idx ON goods(\"timestamp\", height);");
            case 41:
                apply("CREATE TABLE purchase("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    buyer_id BIGINT NOT NULL,"
                    + "    goods_id BIGINT NOT NULL,"
                    + "    seller_id BIGINT NOT NULL,"
                    + "    quantity INT NOT NULL,"
                    + "    price BIGINT NOT NULL,"
                    + "    deadline INT NOT NULL,"
                    + "    note BLOB,"
                    + "    nonce CHAR(64),"
                    + "    \"timestamp\" INT NOT NULL,"
                    + "    pending BOOLEAN NOT NULL,"
                    + "    goods BLOB,"
                    + "    goods_nonce CHAR(64),"
                    + "    refund_note BLOB,"
                    + "    refund_nonce CHAR(64),"
                    + "    has_feedback_notes BOOLEAN DEFAULT FALSE NOT NULL,"
                    + "    has_public_feedbacks BOOLEAN DEFAULT FALSE NOT NULL,"
                    + "    discount BIGINT NOT NULL,"
                    + "    refund BIGINT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(id, height)"
                    + ");");
            case 42:
                apply("CREATE UNIQUE DESCENDING INDEX purchase_id_height_idx ON purchase(id, height);");
            case 43:
                apply("CREATE DESCENDING INDEX purchase_buyer_id_height_idx ON purchase(buyer_id, height);");
            case 44:
                apply("CREATE DESCENDING INDEX purchase_seller_id_height_idx ON purchase(seller_id, height);");
            case 45:
                apply("CREATE DESCENDING INDEX purchase_deadline_idx ON purchase(deadline, height);");
            case 46:
                apply("CREATE DESCENDING INDEX purchase_timestamp_idx ON purchase(\"timestamp\", id);");
            case 47:
                apply("CREATE TABLE account_asset("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    account_id BIGINT NOT NULL,"
                    + "    asset_id BIGINT NOT NULL,"
                    + "    quantity BIGINT NOT NULL,"
                    + "    unconfirmed_quantity BIGINT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
                    + "    UNIQUE(account_id, asset_id, height)"
                    + ");");
            case 48:
                apply("CREATE UNIQUE DESCENDING INDEX account_asset_id_height_idx ON account_asset(account_id, asset_id, height);");
            case 49:
                apply("CREATE DESCENDING INDEX account_asset_quantity_idx ON account_asset(quantity);");
            case 50:
                apply("CREATE TABLE purchase_feedback("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    feedback_data BLOB NOT NULL,"
                    + "    feedback_nonce CHAR(64) NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL"
                    + ");");
            case 51:
                apply("CREATE DESCENDING INDEX " +  maybeToShortIdentifier("purchase_feedback_id_height_idx") + " ON purchase_feedback(id, height);");
            case 52:
                apply("CREATE TABLE purchase_public_feedback("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    public_feedback BLOB SUB_TYPE TEXT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL"
                    + ");");
            case 53:
                apply("CREATE DESCENDING INDEX " + maybeToShortIdentifier("purchase_public_feedback_id_height_idx") + " ON purchase_public_feedback(id, height);");
            case 54:
                apply("CREATE TABLE unconfirmed_transaction("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    expiration INT NOT NULL,"
                    + "    transaction_height INT NOT NULL,"
                    + "    fee_per_byte BIGINT NOT NULL,"
                    + "    \"timestamp\" INT NOT NULL,"
                    + "    transaction_bytes BLOB NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    UNIQUE(id)"
                    + ");");
            case 55:
                apply("CREATE UNIQUE INDEX " + maybeToShortIdentifier("unconfirmed_transaction_id_idx") + " ON unconfirmed_transaction(id);");
            case 56:
                apply("CREATE DESCENDING INDEX " + maybeToShortIdentifier("unconfirmed_transaction_fee_timestamp_idx") + " ON unconfirmed_transaction(transaction_height, fee_per_byte, \"timestamp\");");
            case 57:
                apply("CREATE TABLE asset_transfer("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    asset_id BIGINT NOT NULL,"
                    + "    sender_id BIGINT NOT NULL,"
                    + "    recipient_id BIGINT NOT NULL,"
                    + "    quantity BIGINT NOT NULL,"
                    + "    \"timestamp\" INT NOT NULL,"
                    + "    height INT NOT NULL,"
		    + "    UNIQUE(id)"
                    + ");");
            case 58:
                apply("CREATE UNIQUE INDEX asset_transfer_id_idx ON asset_transfer(id);");
            case 59:
                apply("CREATE DESCENDING INDEX asset_transfer_asset_id_idx ON asset_transfer(asset_id, height);");
            case 60:
                apply("CREATE DESCENDING INDEX asset_transfer_sender_id_idx ON asset_transfer(sender_id, height);");
            case 61:
                apply("CREATE DESCENDING INDEX asset_transfer_recipient_id_idx ON asset_transfer(recipient_id, height);");
            case 62:
                apply("CREATE TABLE reward_recip_assign("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    account_id BIGINT NOT NULL,"
                    + "    prev_recip_id BIGINT NOT NULL,"
                    + "    recip_id BIGINT NOT NULL,"
                    + "    from_height INT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(account_id, height)"
                    + ");");
            case 63:
                apply("CREATE UNIQUE DESCENDING INDEX " + maybeToShortIdentifier("reward_recip_assign_account_id_height_idx") + " ON reward_recip_assign(account_id, height);");
            case 64:
                apply("CREATE DESCENDING INDEX " + maybeToShortIdentifier("reward_recip_assign_recip_id_height_idx") + " ON reward_recip_assign(recip_id, height);");
            case 65:
                apply("CREATE TABLE escrow("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    sender_id BIGINT NOT NULL,"
                    + "    recipient_id BIGINT NOT NULL,"
                    + "    amount BIGINT NOT NULL,"
                    + "    required_signers INT,"
                    + "    deadline INT NOT NULL,"
                    + "    deadline_action INT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(id, height)"
                    + ");");
            case 66:
                apply("CREATE UNIQUE DESCENDING INDEX escrow_id_height_idx ON escrow(id, height);");
            case 67:
                apply("CREATE DESCENDING INDEX escrow_sender_id_height_idx ON escrow(sender_id, height);");
            case 68:
                apply("CREATE DESCENDING INDEX escrow_recipient_id_height_idx ON escrow(recipient_id, height);");
            case 69:
                apply("CREATE DESCENDING INDEX escrow_deadline_height_idx ON escrow(deadline, height);");
            case 70:
                apply("CREATE TABLE escrow_decision("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    escrow_id BIGINT NOT NULL,"
                    + "    account_id BIGINT NOT NULL,"
                    + "    decision int NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(escrow_id, account_id, height)"
                    + ");");
            case 71:
                apply("CREATE UNIQUE DESCENDING INDEX " + maybeToShortIdentifier("escrow_decision_escrow_id_account_id_height_idx") + " ON escrow_decision(escrow_id, account_id, height);");
            case 72:
                apply("CREATE DESCENDING INDEX " + maybeToShortIdentifier("escrow_decision_escrow_id_height_idx") + " ON escrow_decision(escrow_id, height);");
            case 73:
                apply("CREATE DESCENDING INDEX " + maybeToShortIdentifier("escrow_decision_account_id_height_idx") + " ON escrow_decision(account_id, height);");
            case 74:
                apply("CREATE TABLE subscription("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    sender_id BIGINT NOT NULL,"
                    + "    recipient_id BIGINT NOT NULL,"
                    + "    amount BIGINT NOT NULL,"
                    + "    frequency INT NOT NULL,"
                    + "    time_next INT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(id, height)"
                    + ");");
            case 75:
                apply("CREATE UNIQUE INDEX subscription_id_height_idx ON subscription(id, height);");
            case 76:
                apply("CREATE DESCENDING INDEX " + maybeToShortIdentifier("subscription_sender_id_height_idx") + " ON subscription(sender_id, height);");
            case 77:
                apply("CREATE DESCENDING INDEX " + maybeToShortIdentifier("subscription_recipient_id_height_idx") + " ON subscription(recipient_id, height);");
            case 78:
                apply("CREATE TABLE \"AT\"("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    creator_id BIGINT NOT NULL,"
                    + "    name VARCHAR(30),"
                    + "    description BLOB SUB_TYPE TEXT,"
                    + "    version SMALLINT NOT NULL,"
                    + "    csize INT NOT NULL,"
                    + "    dsize INT NOT NULL,"
                    + "    c_user_stack_bytes INT NOT NULL,"
                    + "    c_call_stack_bytes INT NOT NULL,"
                    + "    creation_height INT NOT NULL,"
                    + "    ap_code BLOB NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(id, height)"
                    + ");");
            case 79:
                apply("CREATE UNIQUE DESCENDING INDEX at_id_height_idx ON \"AT\"(id, height);");
            case 80:
                apply("CREATE DESCENDING INDEX at_creator_id_height_idx ON \"AT\"(creator_id, height);");
            case 81:
                apply("CREATE TABLE at_state("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    at_id BIGINT NOT NULL,"
                    + "    state BLOB NOT NULL,"
                    + "    prev_height INT NOT NULL,"
                    + "    next_height INT NOT NULL,"
                    + "    sleep_between INT NOT NULL,"
                    + "    prev_balance BIGINT NOT NULL,"
                    + "    freeze_when_same_balance BOOLEAN NOT NULL,"
                    + "    min_activate_amount BIGINT NOT NULL,"
                    + "    height INT NOT NULL,"
                    + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
		    + "    UNIQUE(at_id, height)"
                    + ");");
	    case 82:
                apply("CREATE UNIQUE DESCENDING INDEX at_state_at_id_height_idx ON at_state(at_id, height);");
            case 83:
                apply("CREATE DESCENDING INDEX " + maybeToShortIdentifier("at_state_id_next_height_height_idx") + " ON at_state(at_id, next_height, height);");
            case 84:
                apply("CREATE TABLE block("
                    + "    db_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                    + "    id BIGINT NOT NULL,"
                    + "    version INT NOT NULL,"
                    + "    \"timestamp\" INT NOT NULL,"
                    + "    previous_block_id BIGINT,"
                    + "    total_amount BIGINT NOT NULL,"
                    + "    total_fee BIGINT NOT NULL,"
                    + "    payload_length INT NOT NULL,"
                    + "    generator_public_key CHAR(64) NOT NULL,"
                    + "    previous_block_hash CHAR(64),"
                    + "    cumulative_difficulty BLOB NOT NULL,"
                    + "    base_target BIGINT NOT NULL,"
                    + "    next_block_id BIGINT,"
                    + "    height INT NOT NULL,"
                    + "    generation_signature CHAR(64) NOT NULL,"
                    + "    block_signature CHAR(128) NOT NULL,"
                    + "    payload_hash CHAR(64) NOT NULL,"
                    + "    generator_id BIGINT NOT NULL,"
                    + "    nonce BIGINT NOT NULL,"
                    + "    ats BLOB,"
		    + "    UNIQUE(id),"
		    + "    UNIQUE(height),"
		    + "    UNIQUE(\"timestamp\")"
                    + ");");
            case 85:
                apply("CREATE UNIQUE INDEX block_id_idx ON block(id);");
            case 86:
                apply("CREATE UNIQUE INDEX block_height_idx ON block(height);");
            case 87:
                apply("CREATE INDEX block_generator_id_idx ON block(generator_id);");
            case 88:
                apply("CREATE UNIQUE DESCENDING INDEX block_timestamp_idx ON block(\"timestamp\");");
            case 89:
		apply("ALTER TABLE transaction ADD CONSTRAINT constraint_ff FOREIGN KEY(block_id) REFERENCES block(id) ON DELETE CASCADE;");
            case 90:
                apply("ALTER TABLE block ADD CONSTRAINT constraint_3c5 FOREIGN KEY(next_block_id) REFERENCES block(id) ON DELETE SET NULL;");
            case 91:
                apply("ALTER TABLE block ADD CONSTRAINT constraint_3c FOREIGN KEY(previous_block_id) REFERENCES block(id) ON DELETE CASCADE;");
            case 92:
                apply("UPDATE version set next_update = '162';");
            case 163:
               	apply("ALTER TABLE alias ALTER COLUMN alias_name_LOWER SET DEFAULT '';");
            case 164:
		/*
            case 164:
               	apply("ALTER DATABASE burstwallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            case 165:
              	apply("ALTER TABLE alias CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            case 166:
               	apply("ALTER TABLE account CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            case 167:
               	apply("ALTER TABLE asset CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            case 168:
               	apply("ALTER TABLE goods CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            case 169:
               	apply("ALTER TABLE at CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
		*/
            case 170:
               	return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");


            /**
             *  IMPORTANT: IF YOU ADD FOREIGN KEYS OR OTHER CONTRAINTS HERE MAKE SURE TO ADD THEM TO FirebirdDbs.java as well!
             *  */

        }
    }

    private FirebirdDbVersion() {} //never
}
