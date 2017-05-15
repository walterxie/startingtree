package test.beast.evolution.tree;

import beast.evolution.tree.FlexibleTree;
import beast.evolution.tree.Node;
import junit.framework.TestCase;

/**
 * @author Walter Xie
 */
public class FlexibleTreeTest extends TestCase {
    String[] trees = new String[]{
            "((((A:1.0,B:1.0):1.0,C:2.0):2.0,D:3.0):3.0,E:5.0);", // binary tree
//            "(A:1.0,B:2.0,(C:3.0,D:4.0):5.0);" // multifurcating tree
    };
    String[] newTrees = new String[]{
            "(A:0.5,(B:1.0,(C:2.0,(D:3.0,E:8.0):2.0):1.0):0.5):0.0;",
            "(B:0.5,((C:2.0,(D:3.0,E:8.0):2.0):1.0,A:1.0):0.5):0.0;",
            "(C:1.0,((D:3.0,E:8.0):2.0,(A:1.0,B:1.0):1.0):1.0):0.0;",
            "(D:1.5,(E:8.0,((A:1.0,B:1.0):1.0,C:2.0):2.0):1.5):0.0;",
            "(E:4.0,(((A:1.0,B:1.0):1.0,C:2.0):2.0,D:3.0):4.0):0.0;",
            "((A:1.0,B:1.0):0.5,(C:2.0,(D:3.0,E:8.0):2.0):0.5):0.0;"
    };

    public void testChangeRootTo() throws Exception {
        String tree = trees[0];
        FlexibleTree flexibleTree = new FlexibleTree(tree);

        System.out.println(flexibleTree.toNewick() + "\n");

        for (int i = 0; i < newTrees.length; i++) {
            // set new root between i and its parent with half length each side
            Node newRoot = flexibleTree.getNode(i);

            System.out.println("Change the root at the lineage " + i + " ascended from " + newRoot.getID());

            flexibleTree.changeRootTo(newRoot, 0.5);
            String newTree = flexibleTree.toNewick();

            System.out.println(newTree);

            if (!newTree.endsWith(";"))
                newTree += ";";

            assertEquals(newTrees[i], newTree);
        }

    }

    public void testSumOfSquaredDistance() throws Exception {

        FlexibleTree flexibleTree = new FlexibleTree(trees[0]);
        System.out.println(flexibleTree.toNewick());

        double ss = flexibleTree.getSumOfSquaredDistance();
        System.out.println("sum of squared distances = " + ss);

        assertEquals(54.0, ss);
    }

    public void testMinSSDTree() throws Exception {

        String minSSDTreeString = "(E:4.0,(((A:1.0,B:1.0):1.0,C:2.0):2.0,D:3.0):4.0):0.0;";

        FlexibleTree flexibleTree = new FlexibleTree(trees[0]);
        System.out.println(flexibleTree.toNewick());

        FlexibleTree minSSDTree = flexibleTree.getMinSSDTree();
        assertEquals(minSSDTree.toNewick(), minSSDTreeString);

        double ss = minSSDTree.getSumOfSquaredDistance();
        assertEquals(54.0, ss);
    }

}
