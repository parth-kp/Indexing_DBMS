//***************************************************************************************************************************************
// DBMS INNOVATIVE ASSIGNMENT: Hashing And Indexing In  DataBase.
//  20BCE195 - PARTH PATEL
//  20BCE203 - DHRUVIL PATEL
//  20BCE204 - DHYAN PATEL
//***************************************************************************************************************************************
import java.sql.*;
import java.util.*;
import java.time.Duration;
import java.time.Instant;
//***************************************************************************************************************************************
class Employee{
    String empid;
    String fname;
    String lname;
    String email;
    String phoneno;
    String salary;
    String depid;
    Employee(String empid,String fname,String lname,String email,String phoneno,String salary,String depid){
        this.empid = empid;
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.phoneno = phoneno;
        this.salary = salary;
        this.depid = depid;
    }
    @Override
    public String toString() {
        return String.format("| %-6s| %-10s| %-10s| %-35s| %-10s| %-8s| %-6s|", this.empid, this.fname, this.lname, this.email, this.phoneno, this.salary, this.depid);
    }
}
//***************************************************************************************************************************************
class BPlusTree {
    int m;
    InternalNode root;
    LeafNode firstLeaf;

    // Binary search program
    private int binarySearch(DictionaryPair[] dps, int numPairs, int t) {
        Comparator<DictionaryPair> c = (o1, o2) -> {
            Integer a = o1.key;
            Integer b = o2.key;
            return a.compareTo(b);
        };
        return Arrays.binarySearch(dps, 0, numPairs, new DictionaryPair(t, new Employee("", "", "", "", "", "", "")), c);
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    // Find the leaf node
    private LeafNode findLeafNode(int key) {

        Integer[] keys = this.root.keys;
        int i;

        for (i = 0; i < this.root.degree - 1; i++) {
            if (key < keys[i]) {
                break;
            }
        }

        Node child = this.root.childPointers[i];
        if (child instanceof LeafNode) {
            return (LeafNode) child;
        } else {
            return findLeafNode((InternalNode) child, key);
        }
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    // Find the leaf node
    private LeafNode findLeafNode(InternalNode node, int key) {

        Integer[] keys = node.keys;
        int i;

        for (i = 0; i < node.degree - 1; i++) {
            if (key < keys[i]) {
                break;
            }
        }
        Node childNode = node.childPointers[i];
        if (childNode instanceof LeafNode) {
            return (LeafNode) childNode;
        } else {
            return findLeafNode((InternalNode) node.childPointers[i], key);
        }
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    // Get the mid point
    private int getMidpoint() {
        return (int) Math.ceil((this.m + 1) / 2.0) - 1;
    }
    //-------------------------------------------------------------------------------------------------------------------------------
private boolean isEmpty() {
        return firstLeaf == null;
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    private int linearNullSearch(DictionaryPair[] dps) {
        for (int i = 0; i < dps.length; i++) {
            if (dps[i] == null) {
                return i;
            }
        }
        return -1;
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    private int linearNullSearch(Node[] pointers) {
        for (int i = 0; i < pointers.length; i++) {
            if (pointers[i] == null) {
                return i;
            }
        }
        return -1;
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    private void sortDictionary(DictionaryPair[] dictionary) {
        Arrays.sort(dictionary, (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return o1.compareTo(o2);
        });
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    private Node[] splitChildPointers(InternalNode in, int split) {

        Node[] pointers = in.childPointers;
        Node[] halfPointers = new Node[this.m + 1];

        for (int i = split + 1; i < pointers.length; i++) {
            halfPointers[i - split - 1] = pointers[i];
            in.removePointer(i);
        }

        return halfPointers;
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    private DictionaryPair[] splitDictionary(LeafNode ln, int split) {

        DictionaryPair[] dictionary = ln.dictionary;

        DictionaryPair[] halfDict = new DictionaryPair[this.m];

        for (int i = split; i < dictionary.length; i++) {
            halfDict[i - split] = dictionary[i];
            ln.delete(i);
        }

        return halfDict;
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    private void splitInternalNode(InternalNode in) {

        InternalNode parent = in.parent;

        int midpoint = getMidpoint();
        int newParentKey = in.keys[midpoint];
        Integer[] halfKeys = splitKeys(in.keys, midpoint);
        Node[] halfPointers = splitChildPointers(in, midpoint);

        in.degree = linearNullSearch(in.childPointers);

        InternalNode sibling = new InternalNode(this.m, halfKeys, halfPointers);
        for (Node pointer : halfPointers) {
            if (pointer != null) {
                pointer.parent = sibling;
            }
        }

        sibling.rightSibling = in.rightSibling;
        if (sibling.rightSibling != null) {
            sibling.rightSibling.leftSibling = sibling;
        }
        in.rightSibling = sibling;
        sibling.leftSibling = in;

        if (parent == null) {

            Integer[] keys = new Integer[this.m];
            keys[0] = newParentKey;
            InternalNode newRoot = new InternalNode(this.m, keys);
            newRoot.appendChildPointer(in);
            newRoot.appendChildPointer(sibling);
            this.root = newRoot;

            in.parent = newRoot;
            sibling.parent = newRoot;

        } else {

            parent.keys[parent.degree - 1] = newParentKey;
            Arrays.sort(parent.keys, 0, parent.degree);

            int pointerIndex = parent.findIndexOfPointer(in) + 1;
            parent.insertChildPointer(sibling, pointerIndex);
            sibling.parent = parent;
        }
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    private Integer[] splitKeys(Integer[] keys, int split) {

        Integer[] halfKeys = new Integer[this.m];

        keys[split] = null;

        for (int i = split + 1; i < keys.length; i++) {
            halfKeys[i - split - 1] = keys[i];
            keys[i] = null;
        }

        return halfKeys;
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    public void insert(int key, Employee value) {
        if (isEmpty()) {

            this.firstLeaf = new LeafNode(this.m, new DictionaryPair(key, value));

        } else {
            LeafNode ln = (this.root == null) ? this.firstLeaf : findLeafNode(key);

            if (!ln.insert(new DictionaryPair(key, value))) {

                ln.dictionary[ln.numPairs] = new DictionaryPair(key, value);
                ln.numPairs++;
                sortDictionary(ln.dictionary);

                int midpoint = getMidpoint();
                DictionaryPair[] halfDict = splitDictionary(ln, midpoint);

                if (ln.parent == null) {

                    Integer[] parent_keys = new Integer[this.m];
                    parent_keys[0] = halfDict[0].key;
                    InternalNode parent = new InternalNode(this.m, parent_keys);
                    ln.parent = parent;
                    parent.appendChildPointer(ln);

                } else {
                    int newParentKey = halfDict[0].key;
                    ln.parent.keys[ln.parent.degree - 1] = newParentKey;
                    Arrays.sort(ln.parent.keys, 0, ln.parent.degree);
                }

                LeafNode newLeafNode = new LeafNode(this.m, halfDict, ln.parent);

                int pointerIndex = ln.parent.findIndexOfPointer(ln) + 1;
                ln.parent.insertChildPointer(newLeafNode, pointerIndex);

                newLeafNode.rightSibling = ln.rightSibling;
                if (newLeafNode.rightSibling != null) {
                    newLeafNode.rightSibling.leftSibling = newLeafNode;
                }
                ln.rightSibling = newLeafNode;
                newLeafNode.leftSibling = ln;

                if (this.root == null) {

                    this.root = ln.parent;

                } else {
                    InternalNode in = ln.parent;
                    while (in != null) {
                        if (in.isOverfull()) {
                            splitInternalNode(in);
                        } else {
                            break;
                        }
                        in = in.parent;
                    }
                }
            }
        }
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    public Employee search(int key) {

        if (isEmpty()) {
            return null;
        }

        LeafNode ln = (this.root == null) ? this.firstLeaf : findLeafNode(key);

        DictionaryPair[] dps = ln.dictionary;
        int index = binarySearch(dps, ln.numPairs, key);

        if (index < 0) {
            return null;
        } else {
            return dps[index].value;
        }
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    public BPlusTree(int m) {
        this.m = m;
        this.root = null;
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    public class Node {
        InternalNode parent;
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    private class InternalNode extends Node {
        int maxDegree;
        int minDegree;
        int degree;
        InternalNode leftSibling;
        InternalNode rightSibling;
        Integer[] keys;
        Node[] childPointers;
        //-------------------------------------------------------------------------------------------------------------------------------

        private void appendChildPointer(Node pointer) {
            this.childPointers[degree] = pointer;
            this.degree++;
        }
        //-------------------------------------------------------------------------------------------------------------------------------

        private int findIndexOfPointer(Node pointer) {
            for (int i = 0; i < childPointers.length; i++) {
                if (childPointers[i] == pointer) {
                    return i;
                }
            }
            return -1;
        }
        //-------------------------------------------------------------------------------------------------------------------------------

        private void insertChildPointer(Node pointer, int index) {
            if (degree - index >= 0) System.arraycopy(childPointers, index, childPointers, index + 1, degree - index);
            this.childPointers[index] = pointer;
            this.degree++;
        }
        //-------------------------------------------------------------------------------------------------------------------------------

        private boolean isOverfull() {
            return this.degree == maxDegree + 1;
        }
        //-------------------------------------------------------------------------------------------------------------------------------

        private void removePointer(int index) {
            this.childPointers[index] = null;
            this.degree--;
        }
        //------------------------------------------------------------------------------------------------------------------------------

        private InternalNode(int m, Integer[] keys) {
            this.maxDegree = m;
            this.minDegree = (int) Math.ceil(m / 2.0);
            this.degree = 0;
            this.keys = keys;
            this.childPointers = new Node[this.maxDegree + 1];
        }
        //-------------------------------------------------------------------------------------------------------------------------------

        private InternalNode(int m, Integer[] keys, Node[] pointers) {
            this.maxDegree = m;
            this.minDegree = (int) Math.ceil(m / 2.0);
            this.degree = linearNullSearch(pointers);
            this.keys = keys;
            this.childPointers = pointers;
        }
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    public class LeafNode extends Node {
        int maxNumPairs;
        int minNumPairs;
        int numPairs;
        LeafNode leftSibling;
        LeafNode rightSibling;
        DictionaryPair[] dictionary;

        public void delete(int index) {
            this.dictionary[index] = null;
            numPairs--;
        }
        //-------------------------------------------------------------------------------------------------------------------------------

        public boolean insert(DictionaryPair dp) {
            if (this.isFull()) {
                return false;
            } else {
                this.dictionary[numPairs] = dp;
                numPairs++;
                Arrays.sort(this.dictionary, 0, numPairs);

                return true;
            }
        }
        //-------------------------------------------------------------------------------------------------------------------------------

        public boolean isFull() {
            return numPairs == maxNumPairs;
        }

        //-------------------------------------------------------------------------------------------------------------------------------

        public LeafNode(int m, DictionaryPair dp) {
            this.maxNumPairs = m - 1;
            this.minNumPairs = (int) (Math.ceil(m / 2) - 1);
            this.dictionary = new DictionaryPair[m];
            this.numPairs = 0;
            this.insert(dp);
        }
        //-------------------------------------------------------------------------------------------------------------------------------

        public LeafNode(int m, DictionaryPair[] dps, InternalNode parent) {
            this.maxNumPairs = m - 1;
            this.minNumPairs = (int) (Math.ceil(m / 2) - 1);
            this.dictionary = dps;
            this.numPairs = linearNullSearch(dps);
            this.parent = parent;
        }
    }
    //-------------------------------------------------------------------------------------------------------------------------------

    public static class DictionaryPair implements Comparable<DictionaryPair> {
        int key;
        Employee value;

        public DictionaryPair(int key, Employee value) {
            this.key = key;
            this.value = value;
        }

        public int compareTo(DictionaryPair o) {
            return Integer.compare(key, o.key);
        }
    }
    //-----------------------------------------------------------------------------------------------------------------------------------
}
//***************************************************************************************************************************************
public class DbmsInnovative {
    static void Hash(HashMap<String, Employee> a, String b) {
        if(!a.containsKey(b)) {
            System.out.println("0 Rows Selected");
            return;
        }
        System.out.println(a.get(b));
    }
    public static void main(String[] args) throws Exception{

        String url = "jdbc:mysql://localhost:3306/parth";
        String uname = "root";
        String pass = "ParthPatel";
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con = DriverManager.getConnection(url,uname,pass);
        Statement st = con.createStatement();
        String Query = "SELECT * FROM EMPLOYEE";
        ResultSet rs = st.executeQuery(Query);
        //-------------------------------------------------------------------------------------------------------------------------------
//        while(rs.next()){
//            System.out.println(rs.getInt(1)+"  "+rs.getString(2)+"  "+rs.getString(3)+"  "+rs.getString(4)+"  "+rs.getInt(5)+"  "+rs.getInt(6));
//        }
        //-------------------------------------------------------------------------------------------------------------------------------
        HashMap<String, Employee> DataHashMap = new HashMap<>();
        BPlusTree bpt;
        bpt = new BPlusTree(3);
        ArrayList<Employee> Data = new ArrayList<>();

        //-------------------------------------------------------------------------------------------------------------------------------
        System.out.println("DATABASE CONTAINS:-\n");
        String str = String.format("| %-6s| %-10s| %-10s| %-35s| %-10s| %-8s| %-6s|","Empid","Fname","Lname","Email","PhoneNo","Salary","Depid");
        System.out.println(str);
        System.out.println("*****************************************************************************************************");
        while (rs.next()) {
            String s = rs.getInt(1)+" "+rs.getString(2)+" "+rs.getString(3)+" "+rs.getString(4)+" "+rs.getInt(5)+" "+rs.getInt(6)+" "+rs.getInt(7);
			//System.out.println(s);
            String[] temp = s.split(" ");
            Employee emp = new Employee(temp[0], temp[1], temp[2],temp[3], temp[4], temp[5], temp[6]);
            DataHashMap.put(emp.empid, emp);
            bpt.insert(Integer.parseInt(emp.empid), emp);
            Data.add(emp);
            System.out.println(emp);
        }
        System.out.println();
        st.close();
        con.close();
        //-------------------------------------------------------------------------------------------------------------------------------
        Scanner sc = new Scanner(System.in);
        while(true) {
            Instant start,end;
            Duration timeElapsed;
            System.out.print("Enter The Employee Id To Search (-1 to exit) : ");
            int n = sc.nextInt();
            if (n == -1) break;
            //-------------------------------------------------------------------------------------------------------------------------------
            System.out.println("\n\n\nHashing");
            System.out.println(str);
            String index= String.valueOf(n);
                start = Instant.now();
                Hash(DataHashMap,index);
                end = Instant.now();
            timeElapsed = Duration.between(start, end);
            System.out.println("Time taken in Searching in Hashing: " + timeElapsed.toMillis() + " milliseconds");

            //-------------------------------------------------------------------------------------------------------------------------------

            System.out.println("\n\n\nB+ Tree Indexing");
            System.out.println(str);
                start = Instant.now();
                System.out.println(bpt.search(n));
                end = Instant.now();
            timeElapsed = Duration.between(start, end);
            System.out.println("Time taken in Searching in B+ Tree: " + timeElapsed.toMillis() + " milliseconds");
            //-------------------------------------------------------------------------------------------------------------------------------


            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url,uname,pass);

            System.out.println("\n\n\nLinear Search");
            System.out.println(str);
            start = Instant.now();
            //System.out.println(linearSearch(Data,n));
            st = conn.createStatement();
            Query = "SELECT * FROM EMPLOYEE WHERE EmployeeID = " + n + ";";
            rs = st.executeQuery(Query);
            rs.next();
            String s = rs.getInt(1) + " " + rs.getString(2) + " " + rs.getString(3) + " " + rs.getString(4) + " " + rs.getInt(5) + " " + rs.getInt(6) + " " + rs.getInt(7);
            String[] temp = s.split(" ");
            Employee emp = new Employee(temp[0], temp[1], temp[2], temp[3], temp[4], temp[5], temp[6]);
            System.out.println(emp);
            end = Instant.now();
            timeElapsed = Duration.between(start, end);
            System.out.println("Time taken in Linear Search: " + timeElapsed.toMillis() + " milliseconds");
            //-------------------------------------------------------------------------------------------------------------------------------
        }
            st.close();
            con.close();

        //-------------------------------------------------------------------------------------------------------------------------------
    }

    private static Employee linearSearch(ArrayList<Employee> data, int n) {
        for (Employee datum : data) {
            if (Objects.equals(datum.empid, String.valueOf(n))) {
                return datum;
            }
        }
        return null;
    }
}
//***************************************************************************************************************************************
