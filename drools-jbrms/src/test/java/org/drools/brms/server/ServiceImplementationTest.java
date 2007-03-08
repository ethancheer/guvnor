package org.drools.brms.server;

import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

import org.drools.brms.client.common.AssetFormats;
import org.drools.brms.client.modeldriven.SuggestionCompletionEngine;
import org.drools.brms.client.modeldriven.brxml.RuleModel;
import org.drools.brms.client.rpc.PackageConfigData;
import org.drools.brms.client.rpc.RepositoryService;
import org.drools.brms.client.rpc.RuleAsset;
import org.drools.brms.client.rpc.RuleContentText;
import org.drools.brms.client.rpc.SnapshotInfo;
import org.drools.brms.client.rpc.TableConfig;
import org.drools.brms.client.rpc.TableDataResult;
import org.drools.brms.client.rpc.TableDataRow;
import org.drools.brms.client.rulelist.AssetItemListViewer;
import org.drools.brms.server.util.TableDisplayHandler;
import org.drools.repository.AssetItem;
import org.drools.repository.CategoryItem;
import org.drools.repository.PackageItem;
import org.drools.repository.StateItem;

import com.google.gwt.user.client.rpc.SerializableException;

public class ServiceImplementationTest extends TestCase {

    
    
  public void testCategory() throws Exception {
        //ServiceImpl impl = new ServiceImpl(new RulesRepository(SessionHelper.getSession()));

        RepositoryService impl = new TestHarnessJBRMSServiceServlet();
      
        String[] originalCats = impl.loadChildCategories( "/" );
        
        Boolean result = impl.createCategory( "/",
                                              "TopLevel1",
                                              "a description" );
        assertTrue( result.booleanValue() );

        result = impl.createCategory( "/",
                                      "TopLevel2",
                                      "a description" );
        assertTrue( result.booleanValue() );
        
        String[] cats = impl.loadChildCategories( "/" );
        assertTrue( cats.length == originalCats.length + 2 );
        
        result = impl.createCategory( "", "Top3", "description" );
        assertTrue(result.booleanValue());
        
        result = impl.createCategory( null, "Top4", "description" );
        assertTrue(result.booleanValue());
        

    }
  
  public void testAddRuleAndListPackages() throws Exception {
      //ServiceImpl impl = new ServiceImpl(new RulesRepository(SessionHelper.getSession()));
      
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      
      impl.repo.loadDefaultPackage();
      impl.repo.createPackage( "another", "woot" );
      
      
      CategoryItem cat = impl.repo.loadCategory( "/" );
      cat.addCategory( "testAddRule", "yeah" );
      
      
      String result = impl.createNewRule( "test AddRule", "a description", "testAddRule", "another", "txt" );
      assertNotNull(result);
      assertFalse("".equals( result ));
      
      PackageConfigData[] packages = impl.listPackages();
      assertTrue(packages.length > 0);
      
      boolean found = false;
      for ( int i = 0; i < packages.length; i++ ) {
          if (packages[i].name.equals( "another" )) {
              found = true;
          }
      }
      
      assertTrue(found);
      
      assertFalse(packages[0].uuid == null);
      assertFalse(packages[0].uuid.equals( "" ));
      
 //just for performance testing with scaling up numbers of rules      
//      for (int i=1; i <= 1000; i++) {
//          impl.createNewRule( "somerule_" + i, "description", 
//                              "testAddRule", "another", "drl" );
//      }
  }

  public void testAttemptDupeRule() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      CategoryItem cat = impl.repo.loadCategory( "/" );
      cat.addCategory( "testAttemptDupeRule", "yeah" );
      
      impl.repo.createPackage("dupes", "yeah");
      
      impl.createNewRule( "testAttemptDupeRule", "ya", "testAttemptDupeRule", "dupes", "rule" );
      
      try {
          impl.createNewRule( "testAttemptDupeRule", "ya", "testAttemptDupeRule", "dupes", "rule" );
          fail("should not allow duplicates.");
      } catch (SerializableException e) {
          assertNotNull(e.getMessage());
      }
      
  }
  
  public void testRuleTableLoad() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      TableConfig conf = impl.loadTableConfig( AssetItemListViewer.RULE_LIST_TABLE_ID );
      assertNotNull(conf.headers);
      
      CategoryItem cat = impl.repo.loadCategory( "/" );
      cat.addCategory( "testRuleTableLoad", "yeah" );
            
      impl.repo.createPackage("testRuleTableLoad", "yeah");      
      impl.createNewRule( "testRuleTableLoad", "ya", "testRuleTableLoad", "testRuleTableLoad", "rule" );
      impl.createNewRule( "testRuleTableLoad2", "ya", "testRuleTableLoad", "testRuleTableLoad", "rule" );

      TableDataResult result = impl.loadRuleListForCategories( "testRuleTableLoad" );
      assertEquals(2, result.data.length);
      
      String key = result.data[0].id;
      assertFalse(key.startsWith( "testRule" ));
      
      assertEquals(result.data[0].format, "rule");
      assertTrue(result.data[0].values[0].startsWith( "testRule" ));
      
      
      
  }
  
  public void testDateFormatting() throws Exception {
      Calendar cal = Calendar.getInstance();
      TableDisplayHandler handler = new TableDisplayHandler();
      String fmt = handler.formatDate( cal );
      assertNotNull(fmt);
      
      assertTrue(fmt.length() > 8);
  }
  
  public void testLoadRuleAsset() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      impl.repo.createPackage( "testLoadRuleAsset", "desc" );
      impl.createCategory( "", "testLoadRuleAsset", "this is a cat" );
      
      
      impl.createNewRule( "testLoadRuleAsset", "description", "testLoadRuleAsset", "testLoadRuleAsset", "drl" );
      
      TableDataResult res = impl.loadRuleListForCategories( "testLoadRuleAsset" );
      assertEquals(1, res.data.length);
      
      TableDataRow row = res.data[0];
      String uuid = row.id;
      

      
      RuleAsset asset = impl.loadRuleAsset( uuid );
      assertNotNull(asset);
      
      assertEquals(uuid, asset.uuid);
      
      assertEquals("description", asset.metaData.description);
      
      
      assertNotNull(asset.content);
      assertTrue(asset.content instanceof RuleContentText);
      assertEquals("testLoadRuleAsset", asset.metaData.name);
      assertEquals("testLoadRuleAsset", asset.metaData.title);
      assertEquals("testLoadRuleAsset", asset.metaData.packageName);
      assertEquals("drl", asset.metaData.format);
      assertNotNull(asset.metaData.createdDate);
      
      assertEquals(1, asset.metaData.categories.length);
      assertEquals("testLoadRuleAsset", asset.metaData.categories[0]);
      
      AssetItem rule = impl.repo.loadPackage( "testLoadRuleAsset" ).loadAsset( "testLoadRuleAsset" );
      impl.repo.createState( "whee" );
      rule.updateState( "whee" );
      rule.checkin( "changed state" );
      asset = impl.loadRuleAsset( uuid );
      
      assertEquals("whee", asset.metaData.status);
      assertEquals("changed state", asset.metaData.checkinComment);
   
      
      uuid = impl.createNewRule( "testBRXMLFormatSugComp", "description", "testLoadRuleAsset", "testLoadRuleAsset", AssetFormats.BUSINESS_RULE );
      asset = impl.loadRuleAsset( uuid );
      assertTrue(asset.content instanceof RuleModel);
      
  }
  
  public void testLoadAssetHistoryAndRestore() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      impl.repo.createPackage( "testLoadAssetHistory", "desc" );
      impl.createCategory( "", "testLoadAssetHistory", "this is a cat" );
      
      
      String uuid = impl.createNewRule( "testLoadAssetHistory", "description", "testLoadAssetHistory", "testLoadAssetHistory", "drl" );
      RuleAsset asset = impl.loadRuleAsset( uuid );
      impl.checkinVersion( asset ); //1
      impl.checkinVersion( asset ); //2
      impl.checkinVersion( asset ); //HEAD   
      
      TableDataResult result = impl.loadAssetHistory( uuid );
      assertNotNull(result);
      TableDataRow[] rows = result.data;
      assertEquals(2, rows.length);
      assertFalse(rows[0].id.equals( uuid ));
      assertFalse(rows[1].id.equals( uuid ));
      
      RuleAsset old = impl.loadRuleAsset( rows[0].id );
      RuleAsset newer = impl.loadRuleAsset( rows[1].id );
      assertFalse(old.metaData.versionNumber.equals( newer.metaData.versionNumber ));
            
      RuleAsset head = impl.loadRuleAsset( uuid );
      
      String oldVersion = old.metaData.versionNumber;
      assertFalse(oldVersion.equals( head.metaData.versionNumber ));
      
      impl.restoreVersion( old.uuid, head.uuid, "this was cause of a mistake" );
      
      RuleAsset newHead = impl.loadRuleAsset( uuid );
      
      
      assertEquals("this was cause of a mistake", newHead.metaData.checkinComment);
      
      
  }
  
  
  
  public void testCheckin() throws Exception {
          TestHarnessJBRMSServiceServlet serv = new TestHarnessJBRMSServiceServlet();
          
          serv.listPackages();
          
          serv.createCategory( "/", "testCheckinCategory", "this is a description" );
          serv.createCategory( "/", "testCheckinCategory2", "this is a description" );
          serv.createCategory( "testCheckinCategory", "deeper", "description" );
          
          String uuid = serv.createNewRule( "testChecking", "this is a description", "testCheckinCategory", "default", "drl" );
          
          RuleAsset asset = serv.loadRuleAsset( uuid );
          
          assertNotNull(asset.metaData.lastModifiedDate);
          
          asset.metaData.coverage = "boo";
          asset.content = new RuleContentText();
          ((RuleContentText) asset.content).content = "yeah !";
          
          Date start = new Date();
          Thread.sleep( 100 );
          
          String uuid2 = serv.checkinVersion( asset );
          assertEquals(uuid, uuid2);
          
          RuleAsset asset2 = serv.loadRuleAsset( uuid );
          assertNotNull(asset2.metaData.lastModifiedDate);
          assertTrue(asset2.metaData.lastModifiedDate.after( start ));
          
          
          assertEquals("boo", asset2.metaData.coverage);
          assertEquals("1", asset2.metaData.versionNumber);
          
          assertEquals("yeah !", ((RuleContentText) asset2.content).content);
          
          asset2.metaData.coverage = "ya";
          asset2.metaData.checkinComment = "checked in";
          
          String cat = asset2.metaData.categories[0];
          asset2.metaData.categories = new String[3];
          asset2.metaData.categories[0] = cat;          
          asset2.metaData.categories[1] = "testCheckinCategory2";
          asset2.metaData.categories[2] = "testCheckinCategory/deeper";
          
          serv.checkinVersion( asset2 );
          
          asset2 = serv.loadRuleAsset( uuid );
          assertEquals("ya", asset2.metaData.coverage);
          assertEquals("2", asset2.metaData.versionNumber);
          assertEquals("checked in", asset2.metaData.checkinComment);
          assertEquals(3, asset2.metaData.categories.length);
          assertEquals("testCheckinCategory", asset2.metaData.categories[0]);
          assertEquals("testCheckinCategory2", asset2.metaData.categories[1]);
          assertEquals("testCheckinCategory/deeper", asset2.metaData.categories[2]);

          
  }
  
  
  public void testCreatePackage() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      PackageConfigData[] pkgs = impl.listPackages();
      String uuid = impl.createPackage( "testCreatePackage", "this is a new package" );
      assertNotNull( uuid );
      
      PackageItem item = impl.repo.loadPackage( "testCreatePackage" );
      assertNotNull(item);
      assertEquals("this is a new package", item.getDescription());
      
      
      
      assertEquals(pkgs.length + 1, impl.listPackages().length);
      
      PackageConfigData conf = impl.loadPackageConfig( uuid );
      assertEquals("this is a new package", conf.description);
      assertNotNull(conf.lastModified);
  }
  
  public void testLoadPackageConfig() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      PackageItem it = impl.repo.loadDefaultPackage();
      String uuid = it.getUUID();
      it.updateCoverage( "xyz" );
      it.updateExternalURI( "ext" );
      it.updateHeader( "header" );
      impl.repo.save();
      
      PackageConfigData data = impl.loadPackageConfig( uuid );
      assertNotNull(data);
      
      assertEquals("default", data.name);
      assertEquals("header", data.header);
      assertEquals("ext", data.externalURI);
      
      assertNotNull(data.uuid);
      
  }
  
  public void testPackageConfSave() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      String uuid = impl.createPackage( "testPackageConfSave", "a desc" );
      PackageConfigData data = impl.loadPackageConfig( uuid );
      
      data.description = "new desc";
      data.header = "wa";
      data.externalURI = "new URI";
      
      
      String uuid2 = impl.savePackage( data );
      assertNotNull(uuid);
      assertEquals(uuid, uuid2);
      
      data = impl.loadPackageConfig( uuid );
      assertEquals("new desc", data.description);
      assertEquals("wa", data.header);
      assertEquals("new URI", data.externalURI);      
  }
  
  public void testListByFormat() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      String cat = "testListByFormat";
      impl.createCategory( "/", cat, "ya" );
      String pkgUUID = impl.createPackage( "testListByFormat", "used for listing by format." );
      
      String uuid = impl.createNewRule( "testListByFormat", "x", cat, "testListByFormat", "testListByFormat" );
      String uuid2 = impl.createNewRule( "testListByFormat2", "x", cat, "testListByFormat", "testListByFormat" );
      String uuid3 = impl.createNewRule( "testListByFormat3", "x", cat, "testListByFormat", "testListByFormat" );
      String uuid4 = impl.createNewRule( "testListByFormat4", "x", cat, "testListByFormat", "testListByFormat" );

      TableDataResult res = impl.listAssets( pkgUUID, arr("testListByFormat"), -1, 0 );
      assertEquals(4, res.data.length);
      assertEquals(uuid, res.data[0].id);
      assertEquals("testListByFormat", res.data[0].values[0]);
      
      res = impl.listAssets( pkgUUID , arr("testListByFormat"), 4, 0 );      
      assertEquals(4, res.data.length);

      res = impl.listAssets( pkgUUID, arr("testListByFormat"), 2, 0 );      
      assertEquals(2, res.data.length);
      assertEquals(uuid, res.data[0].id);
      
      
      res = impl.listAssets( pkgUUID, arr("testListByFormat"), 2, 2 );      
      assertEquals(2, res.data.length);
      assertEquals(uuid3, res.data[0].id);
      
      
      uuid = impl.createNewRule( "testListByFormat5", "x", cat, "testListByFormat", "otherFormat" );
      
      res = impl.listAssets( pkgUUID, arr("otherFormat"), 40, 0 );
      assertEquals(1, res.data.length);
      assertEquals(uuid, res.data[0].id);

      res = impl.listAssets( pkgUUID, new String[] {"otherFormat", "testListByFormat"}, 40, 0 );
      assertEquals(5, res.data.length);
      

      TableDataResult result = impl.quickFindAsset( "testListByForma", 5 );
      assertEquals(5, result.data.length);
      
      assertNotNull(result.data[0].id);
      assertTrue(result.data[0].values[0].startsWith( "testListByFormat" ));
      
      result = impl.quickFindAsset( "testListByForma", 3 );
      assertEquals(4, result.data.length);
      
      assertEquals("MORE", result.data[3].id);
      
      
  }
  
  public String[] arr(String s) {
      return new String[] {s};
  }
  
  public void testStatus() throws Exception {
      TestHarnessJBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      String uuid = impl.createState( "testStatus1" );
      assertNotNull(uuid);
      
      String[] states = impl.listStates();
      assertTrue(states.length > 0);
      
      impl.createState( "testStatus2" );
      String[] states2 = impl.listStates();
      assertEquals(states.length + 1, states2.length);
      
      int match = 0;
      for ( int i = 0; i < states2.length; i++ ) {
         if (states2[i].equals( "testStatus2" )) {
             match++;
         } else if (states2[i].equals( "testStatus1" )) {
             match++;
         }
      }
      
      assertEquals(2, match);
      

      String packagUUID = impl.createPackage( "testStatus", "description" );
      String ruleUUID = impl.createNewRule( "testStatus", "desc", null, "testStatus", "drl" );
      String ruleUUID2 = impl.createNewRule( "testStatus2", "desc", null, "testStatus", "drl" );
      impl.createState( "testState" );
      
      RuleAsset asset = impl.loadRuleAsset( ruleUUID );
      assertEquals(StateItem.DRAFT_STATE_NAME, asset.metaData.status);
      impl.changeState( ruleUUID, "testState", false );
      asset = impl.loadRuleAsset( ruleUUID );
      assertEquals("testState", asset.metaData.status);
      asset = impl.loadRuleAsset( ruleUUID2 );
      assertEquals( StateItem.DRAFT_STATE_NAME, asset.metaData.status);
      
      
      
      impl.createState( "testState2" );
      impl.changeState( packagUUID, "testState2", true );
      
      PackageConfigData pkg = impl.loadPackageConfig( packagUUID );
      assertEquals("testState2", pkg.state);
      
      asset = impl.loadRuleAsset( ruleUUID2 );
      assertEquals("testState2", asset.metaData.status);
      
      impl.checkinVersion( asset );
      asset = impl.loadRuleAsset( asset.uuid );
      assertEquals("testState2", asset.metaData.status);
      
  }
  
  public void testMovePackage() throws Exception {
      JBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      String[] cats = impl.loadChildCategories( "/" );
      if (cats.length == 0) {
          impl.createCategory( "/", "la", "d" );
      }
      String sourcePkgId = impl.createPackage( "sourcePackage", "description" );
      String destPkgId = impl.createPackage( "targetPackage", "description" );
      
      String cat = impl.loadChildCategories( "/" )[0];
      
      String uuid = impl.createNewRule( "testMovePackage", "desc", cat, "sourcePackage", "drl" );
      
      TableDataResult res = impl.listAssets( destPkgId, new String[] {"drl"}, 2, 0 );
      assertEquals(0, res.data.length);
      
      
      impl.changeAssetPackage( uuid, "targetPackage", "yeah" );
      res = impl.listAssets( destPkgId, new String[] {"drl"}, 2, 0 );
      
      assertEquals(1, res.data.length);
      
      res = impl.listAssets( sourcePkgId, new String[] {"drl"}, 2, 0 );
      
      assertEquals(0, res.data.length);
      
    
      
      
  }
  
  public void testCopyAsset() throws Exception {
      JBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      impl.createCategory( "/", "templates", "ya" );
      String uuid = impl.createNewRule( "testCopyAsset", "", "templates", "default", "drl" );
      String uuid2 = impl.copyAsset( uuid, "default", "testCopyAsset2" );
      assertNotSame( uuid, uuid2 );
      
      RuleAsset asset = impl.loadRuleAsset( uuid2 );
      assertNotNull(asset); 
      assertEquals("default", asset.metaData.packageName);
      assertEquals("testCopyAsset2", asset.metaData.name);
  }
  
  public void testSnapshot() throws Exception {
      JBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      impl.createCategory( "/", "snapshotTesting", "y" );
      impl.createPackage( "testSnapshot", "d" );
      String uuid = impl.createNewRule( "testSnapshotRule", "", "snapshotTesting", "testSnapshot", "drl" );
      
      impl.createPackageSnapshot( "testSnapshot", "X", false, "ya" );
      SnapshotInfo[] snaps = impl.listSnapshots( "testSnapshot" );
      assertEquals(1, snaps.length);
      assertEquals("X", snaps[0].name);
      assertEquals("ya", snaps[0].comment);
      assertNotNull(snaps[0].uuid);
      PackageConfigData confSnap = impl.loadPackageConfig( snaps[0].uuid );
      assertEquals("testSnapshot", confSnap.name);
      
      
      impl.createPackageSnapshot( "testSnapshot", "Y", false, "we" );
      assertEquals(2, impl.listSnapshots( "testSnapshot" ).length);
      impl.createPackageSnapshot( "testSnapshot", "X", true, "we" );
      assertEquals(2, impl.listSnapshots( "testSnapshot" ).length);
      
      impl.copyOrRemoveSnapshot( "testSnapshot", "X", false, "Q" );
      assertEquals(3, impl.listSnapshots( "testSnapshot" ).length);
      
      
      
      
      
      try {
          impl.copyOrRemoveSnapshot( "testSnapshot", "X", false, "" );
          fail("should not be able to copy snapshot to empty detination");
      } catch (SerializableException e) {
          assertNotNull(e.getMessage());
      }
      
      impl.copyOrRemoveSnapshot( "testSnapshot", "X", true, null );
      assertEquals(2, impl.listSnapshots( "testSnapshot" ).length);   
      
      
  }
  
  public void testRemoveCategory() throws Exception {
      
      JBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      String[] children = impl.loadChildCategories( "/" );
      impl.createCategory( "/", "testRemoveCategory", "foo" );
      
      impl.removeCategory( "testRemoveCategory" );
      String[] _children = impl.loadChildCategories( "/" );
      assertEquals(children.length, _children.length);
  }
  
  public void testLoadSuggestionCompletionEngine() throws Exception {
      JBRMSServiceServlet impl = new TestHarnessJBRMSServiceServlet();
      String uuid = impl.createPackage( "testSuggestionComp", "x" );
      PackageConfigData conf = impl.loadPackageConfig( uuid );
      conf.header = "import java.util.List";
      
      SuggestionCompletionEngine eng = impl.loadSuggestionCompletionEngine( "testSuggestionComp" );
      assertNotNull(eng);
      
  }
  
    
}
