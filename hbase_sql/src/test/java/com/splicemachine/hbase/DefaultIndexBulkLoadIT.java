package com.splicemachine.hbase;

import com.splicemachine.derby.test.framework.*;
import com.splicemachine.homeless.TestUtils;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.spark_project.guava.base.Throwables;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by yxia on 10/24/17.
 */
public class DefaultIndexBulkLoadIT extends SpliceUnitTest {
    protected static SpliceWatcher spliceClassWatcher = new SpliceWatcher();
    private static final String CLASS_NAME = DefaultIndexBulkLoadIT.class.getSimpleName().toUpperCase();

    protected  static SpliceSchemaWatcher schemaWatcher = new SpliceSchemaWatcher(CLASS_NAME);

    private static SpliceTableWatcher BULK_HFILE_BLANK_TABLE = new SpliceTableWatcher("BULK_HFILE_BLANK_TABLE", schemaWatcher.schemaName,"(i varchar(10) default '', j varchar(10))");
    private static SpliceIndexWatcher BULK_HFILE_BLANK_TABLE_IX = new SpliceIndexWatcher(BULK_HFILE_BLANK_TABLE.tableName, schemaWatcher.schemaName, "BULK_HFILE_BLANK_TABLE_IX",
            schemaWatcher.schemaName, "(i)",false,false,true);
    private static SpliceTableWatcher T1 = new SpliceTableWatcher("T1", schemaWatcher.schemaName, "(a1 int, b1 int default 5, c1 int, d1 varchar(20) default 'NNN', e1 varchar(20))");
    private static SpliceIndexWatcher T1_IX_B1_EXCL_DEFAULTS = new SpliceIndexWatcher(T1.tableName, schemaWatcher.schemaName, "T1_IX_B1_EXCL_DEFAULTS", schemaWatcher.schemaName, "(b1)", false, false, true);
    private static SpliceIndexWatcher T1_IX_C1_EXCL_NULL = new SpliceIndexWatcher(T1.tableName, schemaWatcher.schemaName, "T1_IX_C1_EXCL_NULL", schemaWatcher.schemaName, "(c1)", false, true, false);
    private static SpliceIndexWatcher T1_IX_D1_EXCL_DEFAULTS = new SpliceIndexWatcher(T1.tableName, schemaWatcher.schemaName, "T1_IX_D1_EXCL_DEFAULTS", schemaWatcher.schemaName, "(d1)", false, false, true);
    private static SpliceIndexWatcher T1_IX_E1_EXCL_NULL = new SpliceIndexWatcher(T1.tableName, schemaWatcher.schemaName, "T1_IX_E1_EXCL_NULL", schemaWatcher.schemaName, "(e1)", false, true, false);



    @Rule
    public SpliceWatcher methodWatcher = new SpliceWatcher(schemaWatcher.schemaName);

    @ClassRule
    public static TestRule chain = RuleChain.outerRule(spliceClassWatcher)
            .around(schemaWatcher)
            .around(BULK_HFILE_BLANK_TABLE)
            .around(BULK_HFILE_BLANK_TABLE_IX)
            .around(T1)
            .around(T1_IX_B1_EXCL_DEFAULTS)
            .around(T1_IX_C1_EXCL_NULL)
            .around(T1_IX_D1_EXCL_DEFAULTS)
            .around(T1_IX_E1_EXCL_NULL);

    private Connection conn;

    @Before
    public void setUpTest() throws Exception{
        conn=methodWatcher.getOrCreateConnection();
        conn.setAutoCommit(false);
    }

    @After
    public void tearDownTest() throws Exception{
        try {
            conn.rollback();
        } catch (Exception e) {} // Swallow for HFile Bit Running in Control
    }

    @Before
    public void setUp() throws Exception {
        methodWatcher.executeUpdate(String.format("INSERT INTO T1(a1, c1, e1) VALUES(1, null, null)," +
                "(3, null, null), " +
                "(5, null, null), " +
                "(7, null, null), " +
                "(9, null, null)"));
        methodWatcher.executeUpdate(String.format("INSERT INTO T1 VALUES(2, 2, 2, 'AAA', 'AAA'), " +
                "(4, 4, 4, 'CCC', 'CCC'), " +
                "(6, 6, 6, 'EEE', 'EEE'), " +
                "(8, 8, 8, 'GGG', 'GGG'), " +
                "(10, 10, 10, 'III', 'III')"));
    }

    @Test
    public void testBulkHFileImport() throws Exception {
        try {
            methodWatcher.prepareStatement(format("call SYSCS_UTIL.BULK_IMPORT_HFILE('%s','%s',null,'%s',',','\"',null,null,null,0,null,true,null, '%s', false)", schemaWatcher.schemaName, BULK_HFILE_BLANK_TABLE.tableName
                    , getResourceDirectory() + "null_and_blanks.csv", getResourceDirectory() + "data")).execute();

            try {
                methodWatcher.executeQuery(String.format("SELECT * FROM BULK_HFILE_BLANK_TABLE --SPLICE-PROPERTIES index=BULK_HFILE_BLANK_TABLE_IX\n where i =''"));
                Assert.fail("did not throw exception");
            } catch (SQLException sqle) {
                Assert.assertEquals("No valid execution plan was found for this statement. This is usually because an infeasible join strategy was chosen, or because an index was chosen which prevents the chosen join strategy from being used.", sqle.getMessage());
            }
            ResultSet rs = methodWatcher.executeQuery(String.format("SELECT * FROM BULK_HFILE_BLANK_TABLE --SPLICE-PROPERTIES index=BULK_HFILE_BLANK_TABLE_IX\n where i ='SD'"));


            Assert.assertEquals("I | J |\n" +
                    "--------\n" +
                    "SD |SD |", TestUtils.FormattedResult.ResultFactory.toString(rs));
        }
        catch (Exception e) {
            java.lang.Throwable ex = Throwables.getRootCause(e);
            if (ex.getMessage().contains("bulk load not supported")) {
                // swallow (Control Tests)
            } else {
                throw e;
            }
        }
    }


    @Test
    public void testBulkDelete() throws Exception {
        try {
            methodWatcher.executeUpdate(format("delete from T1 --splice-properties bulkDeleteDirectory='%s", getResourceDirectory() + "data"));

            ResultSet rs = methodWatcher.executeQuery("select * from t1");

            Assert.assertEquals("", TestUtils.FormattedResult.ResultFactory.toString(rs));

            rs = methodWatcher.executeQuery(String.format("SELECT * FROM T1 --SPLICE-PROPERTIES index=T1_IX_B1_EXCL_DEFAULTS\n where b1 <> 5"));

            Assert.assertEquals("", TestUtils.FormattedResult.ResultFactory.toString(rs));
        }
        catch (Exception e) {
            java.lang.Throwable ex = Throwables.getRootCause(e);
            if (ex.getMessage().contains("bulk load not supported")) {
                // swallow (Control Tests)
            } else {
                throw e;
            }
        }
    }
}
