package com.rapleaf.jack;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.rapleaf.jack.queries.AggregatorFunctions;
import com.rapleaf.jack.queries.where_operators.JackMatchers;
import com.rapleaf.jack.test_project.DatabasesImpl;
import com.rapleaf.jack.test_project.IDatabases;
import com.rapleaf.jack.test_project.MockDatabasesImpl;
import com.rapleaf.jack.test_project.database_1.iface.IUserPersistence;
import com.rapleaf.jack.test_project.database_1.models.User;

import static com.rapleaf.jack.queries.AggregatorFunctions.max;
import static com.rapleaf.jack.queries.QueryOrder.ASC;
import static com.rapleaf.jack.queries.QueryOrder.DESC;
import static com.rapleaf.jack.queries.where_operators.JackMatchers.*;


public class TestModelQuery extends TestCase {

  private static final DatabaseConnection DATABASE_CONNECTION1 = new DatabaseConnection("database1");

  public void testDbImplQueries() throws IOException, SQLException {
    IDatabases dbs = new DatabasesImpl(DATABASE_CONNECTION1);
    runAllTests(dbs);
  }

  public void testMockDbQueries() throws IOException, SQLException {
    IDatabases dbs = new MockDatabasesImpl();
    testBasicQuery(dbs);
    testQueryOperators(dbs);
    testQueryById(dbs);
    testQueryWithOrder(dbs);
    testQueryByIdWithOrder(dbs);
    testQueryWithLimit(dbs);
  }

  public void runAllTests(IDatabases dbs) throws IOException, SQLException {
    testBasicQuery(dbs);
    testQueryOperators(dbs);
    testQueryById(dbs);
    testQueryWithOrder(dbs);
    testQueryByIdWithOrder(dbs);
    testQueryWithLimit(dbs);
    testQueryWithSelect(dbs);
    testGroupBy(dbs);
  }

  public void testBasicQuery(IDatabases dbs) throws IOException, SQLException {

    IUserPersistence users = dbs.getDatabase1().users();
    users.deleteAll();

    User userA = users.createDefaultInstance().setHandle("A").setBio("Trader").setNumPosts(1);
    User userB = users.createDefaultInstance().setHandle("B").setBio("Trader").setNumPosts(2);
    User userC = users.createDefaultInstance().setHandle("C").setBio("CEO").setNumPosts(2);
    User userD = users.createDefaultInstance().setHandle("D").setBio("Janitor").setNumPosts(3);
    userA.save();
    userB.save();
    userC.save();
    userD.save();

    Set<User> result;

    // an empty query should return an empty set
    result = users.query().find();
    assertTrue(result.isEmpty());

    // A simple query
    result = users.query().bio("Janitor").find();
    assertEquals(1, result.size());
    assertTrue(result.contains(userD));

    // A chained query
    result = users.query().handle("A").bio("Trader").numPosts(1).find();
    assertEquals(1, result.size());
    assertTrue(result.contains(userA));

    // A chained query
    result = users.query().bio("Trader").numPosts(2).find();
    assertEquals(1, result.size());
    assertTrue(result.contains(userB));

    // A query with no results
    result = users.query().numPosts(3).bio("CEO").find();
    assertTrue(result.isEmpty());
  }

  public void testQueryOperators(IDatabases dbs) throws IOException, SQLException {

    IUserPersistence users = dbs.getDatabase1().users();
    users.deleteAll();

    User brad = users.createDefaultInstance().setHandle("Brad").setBio("Soccer player").setNumPosts(1).setCreatedAtMillis(1l);
    User brandon = users.createDefaultInstance().setHandle("Brandon").setBio("Formula 1 driver").setNumPosts(2).setCreatedAtMillis(1l).setSomeDatetime(0l);
    User casey = users.createDefaultInstance().setHandle("Casey").setBio("Singer").setNumPosts(2).setCreatedAtMillis(2l);
    User john = users.createDefaultInstance().setHandle("John").setBio("Ice skater").setNumPosts(3).setCreatedAtMillis(2l);
    User james = users.createDefaultInstance().setHandle("James").setBio("Surfer").setNumPosts(5).setCreatedAtMillis(3l).setSomeDatetime(1000000l);
    brad.save();
    brandon.save();
    casey.save();
    john.save();
    james.save();

    Set<User> result;

    // Equal To
    result = users.query().whereHandle(equalTo("Brad")).find();
    assertEquals(1, result.size());
    assertTrue(result.contains(brad));

    // Between
    result = users.query().whereNumPosts(between(4, 8)).find();
    assertEquals(1, result.size());
    assertTrue(result.contains(james));

    // Less Than
    result = users.query().whereCreatedAtMillis(lessThan(2l)).find();
    assertEquals(2, result.size());
    assertTrue(result.contains(brad));
    assertTrue(result.contains(brandon));

    // Greater Than
    result = users.query().whereCreatedAtMillis(greaterThan(1l)).find();
    assertEquals(3, result.size());

    // Less Than Or Equal To
    result = users.query().whereCreatedAtMillis(lessThanOrEqualTo(2l)).find();
    assertEquals(4, result.size());

    // Greater Than Or Equal To
    result = users.query().whereCreatedAtMillis(greaterThanOrEqualTo(1l)).find();
    assertEquals(5, result.size());

    // Ends With
    result = users.query().whereBio(endsWith("er")).find();
    assertEquals(5, result.size());

    // StartsWith
    result = users.query().whereBio(startsWith("er")).find();
    assertTrue(result.isEmpty());

    // In with empty collection
    result = users.query().whereSomeDatetime(in(Collections.<Long>emptySet()))
        .find();
    assertTrue(result.isEmpty());

    // NotIn with empty collection
    try {
      users.query().whereSomeDatetime(notIn(Collections.<Long>emptySet()))
          .find();
      fail("Using a NotIn operator with an empty collection should throw an exception.");
    } catch (IllegalArgumentException e) {
      //This is expected
    }

    // Contains and In
    result = users.query().whereBio(contains("f"))
        .whereNumPosts(in(1, 3, 5))
        .find();
    assertEquals(1, result.size());
    assertTrue(result.contains(james));

    // Not In and Not Equal To
    result = users.query().whereHandle(notIn("Brad", "Brandon", "Jennifer", "John"))
        .whereNumPosts(notEqualTo(5))
        .find();
    assertEquals(1, result.size());
    assertTrue(result.contains(casey));

    result = users.query().whereSomeDatetime(JackMatchers.<Long>isNull()).find();
    assertEquals(3, result.size());

    result = users.query().whereSomeDatetime(JackMatchers.<Long>isNotNull()).find();
    assertEquals(2, result.size());
    assertTrue(result.contains(brandon));
    assertTrue(result.contains(james));

    // If a null parameter is passed, an exeception should be thrown
    try {
      users.query().whereHandle(in(null, "brandon")).find();
      fail("an In query with one null parameter should throw an exception");
    } catch (IllegalArgumentException e) {
      // This exception is expected
    }
  }

  public void testQueryById(IDatabases dbs) throws IOException, SQLException {
    IUserPersistence users = dbs.getDatabase1().users();
    users.deleteAll();

    User[] sampleUsers = new User[5];
    for (int i = 0; i < sampleUsers.length; i++) {
      sampleUsers[i] = users.createDefaultInstance().setNumPosts(i % 2);
      sampleUsers[i].save();
    }

    Set<User> result;

    // Query by one id
    result = users.query().id(sampleUsers[0].getId()).find();
    assertEquals(1, result.size());
    assertTrue(result.contains(sampleUsers[0]));

    // Query by several ids
    Set<Long> sampleIds = new HashSet<Long>();
    sampleIds.add(sampleUsers[0].getId());
    sampleIds.add(sampleUsers[3].getId());
    sampleIds.add(sampleUsers[4].getId());
    result = users.query().idIn(sampleIds).find();
    assertEquals(3, result.size());
    assertTrue(result.contains(sampleUsers[0]));
    assertTrue(result.contains(sampleUsers[3]));
    assertTrue(result.contains(sampleUsers[4]));

    //Query by several ids and constraints
    Set<Long> sampleIds2 = new HashSet<Long>();
    sampleIds2.add(sampleUsers[2].getId());
    sampleIds2.add(sampleUsers[3].getId());

    result = users.query()
        .whereNumPosts(greaterThan(0))
        .idIn(sampleIds2)
        .find();
    assertEquals(1, result.size());
    assertTrue(result.contains(sampleUsers[3]));
  }

  public void testQueryWithOrder(IDatabases dbs) throws IOException, SQLException {

    IUserPersistence users = dbs.getDatabase1().users();
    users.deleteAll();

    User userA = users.createDefaultInstance().setHandle("A").setBio("CEO").setNumPosts(1).setSomeDecimal(0.9);
    User userB = users.createDefaultInstance().setHandle("B").setBio("Engineer").setNumPosts(2).setSomeDecimal(12.1);
    User userC = users.createDefaultInstance().setHandle("C").setBio("Analyst").setNumPosts(3).setSomeDecimal(-0.8);
    User userD = users.createDefaultInstance().setHandle("D").setBio("Dean").setNumPosts(3).setSomeDecimal(0.9);
    User userE = users.createDefaultInstance().setHandle("E").setBio("Associate").setNumPosts(3).setSomeDecimal(1.1);
    User userF = users.createDefaultInstance().setHandle("F").setBio("Associate").setNumPosts(6).setSomeDecimal(1.0);
    User userG = users.createDefaultInstance().setHandle("G").setBio("Associate").setNumPosts(5).setSomeDecimal(2.0);
    User userH = users.createDefaultInstance().setHandle("H").setBio("Associate").setNumPosts(7).setSomeDecimal(0.0);
    userA.save();
    userB.save();
    userC.save();
    userD.save();
    userE.save();
    userF.save();
    userG.save();
    userH.save();

    List<User> orderedResult1;
    List<User> orderedResult2;

    // An empty query should return an empty list.
    orderedResult1 = users.query().findWithOrder();
    assertTrue(orderedResult1.isEmpty());

    // A query with no results should return an empty list.
    orderedResult1 = users.query().numPosts(3).bio("CEO").order().findWithOrder();
    assertTrue(orderedResult1.isEmpty());

    // A simple query with single result should return a list with one element.
    orderedResult1 = users.query().bio("Analyst").order().findWithOrder();
    assertEquals(1, orderedResult1.size());
    assertTrue(orderedResult1.contains(userC));

    // A chained query with single result should return a list with one element.
    orderedResult1 = users.query().handle("A").bio("CEO").numPosts(1).order().findWithOrder();
    assertEquals(1, orderedResult1.size());
    assertTrue(orderedResult1.contains(userA));

    // A chained query ordered by default should be ordered by id in an ascending manner.
    // expected result: [userC, userD, userE]
    orderedResult1 = users.query().numPosts(3).order().findWithOrder();
    orderedResult2 = users.query().numPosts(3).orderById(ASC).findWithOrder();
    assertEquals(3, orderedResult1.size());
    assertEquals(0, orderedResult1.indexOf(userC));
    assertEquals(1, orderedResult1.indexOf(userD));
    assertEquals(2, orderedResult1.indexOf(userE));
    assertTrue(orderedResult1.equals(orderedResult2));

    // A chained query ordered by default in a descending manner should be ordered by id in an descending manner.
    // expected result: [userE, userD, userC]
    orderedResult1 = users.query().numPosts(3).order(DESC).findWithOrder();
    orderedResult2 = users.query().numPosts(3).orderById(DESC).findWithOrder();
    assertEquals(3, orderedResult1.size());
    assertEquals(2, orderedResult1.indexOf(userC));
    assertEquals(1, orderedResult1.indexOf(userD));
    assertEquals(0, orderedResult1.indexOf(userE));
    assertTrue(orderedResult1.equals(orderedResult2));

    // A chained query with multiple results ordered by a specific field by default should be ordered in an ascending manner.
    // expected result: [userC, userE, userD]
    orderedResult1 = users.query().numPosts(3).orderByBio().findWithOrder();
    orderedResult2 = users.query().numPosts(3).orderByBio(ASC).findWithOrder();
    assertEquals(3, orderedResult1.size());
    assertEquals(0, orderedResult1.indexOf(userC));
    assertEquals(1, orderedResult1.indexOf(userE));
    assertEquals(2, orderedResult1.indexOf(userD));
    assertTrue(orderedResult1.equals(orderedResult2));

    // A chained query ordered by a specified field in a descending manner should be ordered accordingly.
    // expected result: [userD, userE, userC]
    orderedResult1 = users.query().numPosts(3).orderByBio(DESC).findWithOrder();
    assertEquals(3, orderedResult1.size());
    assertEquals(2, orderedResult1.indexOf(userC));
    assertEquals(1, orderedResult1.indexOf(userE));
    assertEquals(0, orderedResult1.indexOf(userD));

    // a chained ordered query ordered by multiple fields should be ordered accordingly.
    // expected result: [userA, userB, userC, userE, userD, userG, userF, userH]
    orderedResult1 = users.query().whereNumPosts(greaterThan(0)).orderByNumPosts(ASC).orderByBio(ASC).findWithOrder();
    assertEquals(8, orderedResult1.size());
    assertEquals(0, orderedResult1.indexOf(userA));
    assertEquals(1, orderedResult1.indexOf(userB));
    assertEquals(2, orderedResult1.indexOf(userC));
    assertEquals(3, orderedResult1.indexOf(userE));
    assertEquals(4, orderedResult1.indexOf(userD));
    assertEquals(5, orderedResult1.indexOf(userG));
    assertEquals(6, orderedResult1.indexOf(userF));
    assertEquals(7, orderedResult1.indexOf(userH));

    // a chained ordered query ordered by multiple fields should be ordered accordingly.
    // expected result: [C, H, D, A, F, E, G, B]
    orderedResult1 = users.query().whereNumPosts(greaterThan(0)).orderBySomeDecimal().orderByBio(DESC).findWithOrder();
    assertEquals(8, orderedResult1.size());
    assertEquals(0, orderedResult1.indexOf(userC));
    assertEquals(1, orderedResult1.indexOf(userH));
    assertEquals(2, orderedResult1.indexOf(userD));
    assertEquals(3, orderedResult1.indexOf(userA));
    assertEquals(4, orderedResult1.indexOf(userF));
    assertEquals(5, orderedResult1.indexOf(userE));
    assertEquals(6, orderedResult1.indexOf(userG));
    assertEquals(7, orderedResult1.indexOf(userB));
  }

  public void testQueryByIdWithOrder(IDatabases dbs) throws IOException, SQLException {
    IUserPersistence users = dbs.getDatabase1().users();
    users.deleteAll();

    User[] sampleUsers = new User[5];
    for (int i = 0; i < sampleUsers.length; i++) {
      sampleUsers[i] = users.createDefaultInstance().setNumPosts(i % 2);
      sampleUsers[i].save();
    }

    List<User> orderedResult1;
    List<User> orderedResult2;

    // A query by one id should return a list with one element.
    orderedResult1 = users.query().id(sampleUsers[0].getId()).order().findWithOrder();
    assertEquals(1, orderedResult1.size());
    assertTrue(orderedResult1.contains(sampleUsers[0]));

    // A query with an empty collection of id should return nothing
    orderedResult1 = users.query()
        .idIn(Collections.<Long>emptySet())
        .orderByNumPosts()
        .limit(1)
        .findWithOrder();
    assertTrue(orderedResult1.isEmpty());

    // A query by several ids ordered by default should return a list ordered by id in an ascending manner.
    Set<Long> sampleIds = new HashSet<Long>();
    sampleIds.add(sampleUsers[0].getId());
    sampleIds.add(sampleUsers[1].getId());
    sampleIds.add(sampleUsers[2].getId());
    orderedResult1 = users.query().idIn(sampleIds).order().findWithOrder();
    orderedResult2 = users.query().idIn(sampleIds).orderById(ASC).findWithOrder();
    assertEquals(3, orderedResult1.size());
    assertEquals(0, orderedResult1.indexOf(sampleUsers[0]));
    assertEquals(1, orderedResult1.indexOf(sampleUsers[1]));
    assertEquals(2, orderedResult1.indexOf(sampleUsers[2]));
    assertEquals(orderedResult1, orderedResult2);

    // A query by several ids ordered by a specific field should return a list ordered accordingly.
    sampleIds.add(sampleUsers[3].getId());
    sampleIds.add(sampleUsers[4].getId());
    orderedResult1 = users.query().idIn(sampleIds).orderByNumPosts(DESC).orderById(DESC).findWithOrder();
    assertEquals(5, orderedResult1.size());
    assertEquals(0, orderedResult1.indexOf(sampleUsers[3]));
    assertEquals(1, orderedResult1.indexOf(sampleUsers[1]));
    assertEquals(2, orderedResult1.indexOf(sampleUsers[4]));
    assertEquals(3, orderedResult1.indexOf(sampleUsers[2]));
    assertEquals(4, orderedResult1.indexOf(sampleUsers[0]));
  }

  public void testQueryWithLimit(IDatabases dbs) throws IOException, SQLException {
    IUserPersistence users = dbs.getDatabase1().users();
    users.deleteAll();

    int nbUsers = 10;
    User[] sampleUsers = new User[nbUsers];

    for (int i = 0; i < 10; i++) {
      sampleUsers[i] = users.createDefaultInstance().setNumPosts(i);
      sampleUsers[i].save();
    }

    List<User> resultList;

    resultList = users.query()
        .whereNumPosts(lessThan(5))
        .orderByNumPosts()
        .limit(3)
        .findWithOrder();

    assertEquals(3, resultList.size());
    for (int i = 0; i < resultList.size(); i++) {
      assertEquals(i, resultList.get(i).getNumPosts());
    }

    resultList = users.query()
        .whereNumPosts(greaterThan(3))
        .orderByNumPosts()
        .limit(2, 3)
        .findWithOrder();

    assertEquals(3, resultList.size());
    for (int i = 0; i < resultList.size(); i++) {
      assertEquals(i + 6, resultList.get(i).getNumPosts());
    }

    Set<User> resultSet;

    resultSet = users.query()
        .whereNumPosts(lessThan(5))
        .orderByNumPosts()
        .limit(3)
        .find();

    assertEquals(3, resultSet.size());

    resultSet = users.query()
        .whereNumPosts(greaterThan(3))
        .orderByNumPosts()
        .limit(2, 3)
        .find();

    assertEquals(3, resultSet.size());
  }

  public void testQueryWithSelect(IDatabases dbs) throws IOException, SQLException {

    IUserPersistence users = dbs.getDatabase1().users();
    users.deleteAll();

    User userA = users.createDefaultInstance().setHandle("AAAA").setBio("Batman").setCreatedAtMillis(1L).setNumPosts(1);
    User userB = users.createDefaultInstance().setHandle("BBBB").setBio("Superman").setCreatedAtMillis(1L);
    User userC = users.createDefaultInstance().setHandle("CCCC").setBio("Spiderman").setCreatedAtMillis(1L);
    userA.save();
    userB.save();
    userC.save();

    Collection<User> result;

    result = users.query().select(User._Fields.handle)
        .whereBio(endsWith("man"))
        .find();

    for (User user : result) {
      assertTrue(user.getHandle() != null);
      assertTrue(user.getBio() == null);
      assertTrue(user.getCreatedAtMillis() == null);
    }

    result = users.query().select(User._Fields.handle, User._Fields.created_at_millis)
        .whereBio(endsWith("man"))
        .findWithOrder();

    for (User user : result) {
      assertTrue(user.getHandle() != null);
      assertTrue(user.getCreatedAtMillis() != null);
      assertTrue(user.getBio() == null);
    }

    result = users.query().select(User._Fields.created_at_millis)
        .whereBio(endsWith("man"))
        .findWithOrder();

    for (User user : result) {
      assertTrue(user.getHandle().equals(""));
      assertTrue(user.getCreatedAtMillis() != null);
      assertTrue(user.getBio() == null);
    }
  }

  public void testGroupBy(IDatabases dbs) throws IOException, SQLException {
    IUserPersistence users = dbs.getDatabase1().users();
    users.deleteAll();

    for (int i = 0; i < 100; i++) {
      User user = users.createDefaultInstance().setHandle(String.valueOf(i % 2)).setNumPosts(i);
      user.save();
    }

    List<User> result;

    // Test Max
    result = users.query()
        .select(User._Fields.handle)
        .selectAgg(max(User._Fields.num_posts))
        .groupBy(User._Fields.handle)
        .orderByHandle()
        .findWithOrder();

    assertEquals(2, result.size());
    assertEquals("0", result.get(0).getHandle());
    assertEquals(98, result.get(0).getNumPosts());
    assertEquals("1", result.get(1).getHandle());
    assertEquals(99, result.get(1).getNumPosts());

    // Test Min
    result = users.query()
        .select(User._Fields.handle)
        .selectAgg(AggregatorFunctions.min(User._Fields.num_posts))
        .groupBy(User._Fields.handle)
        .orderByHandle()
        .findWithOrder();

    assertEquals(0, result.get(0).getNumPosts());
    assertEquals(1, result.get(1).getNumPosts());


    // Test Count
    result = users.query()
        .select(User._Fields.handle)
        .selectAgg(AggregatorFunctions.count(User._Fields.num_posts))
        .groupBy(User._Fields.handle)
        .orderByHandle()
        .findWithOrder();

    assertEquals(50, result.get(0).getNumPosts());
    assertEquals(50, result.get(1).getNumPosts());

    // Test Sum
    result = users.query()
        .select(User._Fields.handle)
        .selectAgg(AggregatorFunctions.sum(User._Fields.num_posts))
        .groupBy(User._Fields.handle)
        .orderByHandle()
        .findWithOrder();

    assertEquals(2, result.size());
    assertEquals(2450, result.get(0).getNumPosts());
    assertEquals(2500, result.get(1).getNumPosts());

    // Test Avg
    result = users.query()
        .select(User._Fields.handle)
        .selectAgg(AggregatorFunctions.avg(User._Fields.num_posts))
        .groupBy(User._Fields.handle)
        .orderByHandle()
        .findWithOrder();

    assertEquals(2, result.size());
    assertEquals(49, result.get(0).getNumPosts());
    assertEquals(50, result.get(1).getNumPosts());
  }
}
