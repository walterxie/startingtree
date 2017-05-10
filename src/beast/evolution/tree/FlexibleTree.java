package beast.evolution.tree;

import beast.core.Description;

import java.util.ArrayList;
import java.util.List;

@Description("Tree can be changed, such as re-root. Imported from BEAST 1 FlexibleTree.")
public class FlexibleTree extends Tree {

    // Tree class does not support setLength
    protected double[] allBranchLengths; // index is Nr


    public FlexibleTree(final String newick) {
        super(newick, false);
    }

    public FlexibleTree(final Node rootNode) {
        super(rootNode);
    }

    public void setAllBranchLengths() {
        allBranchLengths = getAllBranchLengths(getRoot(), getNodeCount());
    }

    public double[] getAllBranchLengths() {
        if (allBranchLengths == null)
            setAllBranchLengths();
        return allBranchLengths;
    }

    public double getLength(Node node) {
        if (allBranchLengths == null)
            setAllBranchLengths();
        int nodeNr = node.getNr();
        return allBranchLengths[nodeNr];
    }

    public void setLength(Node node, double branchLength) {
        int nodeNr = node.getNr();
        allBranchLengths[nodeNr] = branchLength;
    }

    /**
     * Get all branch lengths of sub/tree.
     * If lengths[nodeNr] == 0, then either is root or not child node
     *
     * @param node a given node, such as <code>getRoot()</code>
     * @param maxNr the max index of all nodes, such as <code>getNodeCount()</code>
     * @return
     */
    private double[] getAllBranchLengths(final Node node, int maxNr) {
        double[] lengths = new double[maxNr];
        List<Node> allChildNodes = node.getAllChildNodes();
        for (Node child : allChildNodes) {
            int nodeNr = child.getNr();
            double branchLength = child.getLength();
//            if (lengths[nodeNr] > 0)
//                throw new IllegalArgumentException("Duplicate node Nr is invalid !");
            lengths[nodeNr] = branchLength;
        }
        return lengths;
    }


    /**
     * Get the maximum node height of the sub/tree including given <code>node</code>
     *
     * @param node the root of the given tree or subtree
     * @return
     */
    public static double getMaxNodeHeight(Node node) {
        if (!node.isLeaf()) {
            double maxNodeHeight = 0;
            for (Node child : node.getAllChildNodes()) {
                double childHeight = child.getHeight();
                if (maxNodeHeight < childHeight)
                    maxNodeHeight = childHeight;
            }
            return maxNodeHeight;
        } else return node.getHeight();
    }


    /**
     * Re-root the tree on the branch above the given <code>node</code>
     * with the given new root.
     * <code>len(node, new_root) = len(node, parent) * propLen </code>
     *
     * @param node the new root
     * @param propLen the proportion of the branch length between <code>node</code>
     *                and its parent node to define the new root, such as 0.5.
     */
    public void changeRootTo(Node node, double propLen) {
        // todo remove restriction to binary tree
        if (!TreeUtils.isBinary(this))
            throw new IllegalArgumentException("changeRootTo is only available to binary tree !");

        Node parent = node.getParent();
        if (parent == null || parent == root) {
            // the node is already the root so nothing to do...
            return;
        }

        hasStartedEditing = true;
        // todo m_tree.getState() == null
//        startEditing(null); // called in rm / add

        setAllBranchLengths();

        Node parent2 = parent.getParent();

        // only change topology
        swapParentNode(parent, parent2, null);

        // the root is now free so use it as the root again
        parent.removeChild(node);
        getRoot().addChild(node);
        getRoot().addChild(parent);
        // adjust lengths for children of new root
        double nodeToParent = getLength(node);
        // setLength change getLength(node)
        setLength(node, nodeToParent * propLen);
        setLength(parent, nodeToParent * (1 - propLen));

        setNodeHeightsByLengths(node, parent, propLen);

        hasStartedEditing = false; // todo is it correct to use restore()? no proposal
    }

    /**
     * Set the node heights from the given branch lengths.
     */
    private void setNodeHeightsByLengths(Node child1, Node child2, double propLen) {

        nodeLengthsToHeights(getRoot(), 0.0);

        double maxHeight = FlexibleTree.getMaxNodeHeight(getRoot());

        for (int i = 0; i < getNodeCount(); i++) {
            Node node = getNode(i);
            // Set the node heights to the reversed heights
            node.setHeight(maxHeight - node.getHeight());
        }

    }

    /**
     * Set the node heights from the current node branch lengths.
     * Actually sets distance from root so the heights then need to be reversed.
     */
    private void nodeLengthsToHeights(Node node, double height) {

        // getLength call setAllBranchLengths() in the first time
        double branchLength = getLength(node);
        if (branchLength > 0.0)
            height += branchLength;

        node.setHeight(height);

        for (Node child : node.getChildren()) {
            nodeLengthsToHeights(child, height);
        }

    }

    /**
     * Work up through the tree putting the parent into the child.
     */
    private void swapParentNode(Node node, Node parent, Node child) {

        if (parent != null) {
            Node parent2 = parent.getParent();

            swapParentNode(parent, parent2, node);

            if (child != null) {
                node.removeChild(child);
                child.addChild(node);
                setLength(node, getLength(child));
            }

        } else {
            // First remove child from the root
            node.removeChild(child);

            // can't remove from list if browsing it with "for each" loop
            List<Node> children = new ArrayList<>(node.getChildren());

            int numChild = children.size();
            if (numChild > 1) {
                // todo insert new internal node in the same position of old root for > 2 children
//                Node newInternalNode = new Node();
                for (int i=0; i<numChild; i++) {
                    Node tmp = children.get(i);
                    node.removeChild(tmp);
                    child.addChild(tmp);
                    setLength(tmp, getLength(tmp) + getLength(child));
                }
            } else {
                Node tmp = children.get(0);
                node.removeChild(tmp);
                child.addChild(tmp);
                setLength(tmp, getLength(tmp) + getLength(child));
            }
        }

    }


    /**
     * replace BEAST2 <code>String toNewick(boolean onlyTopology)</code>
     * in <code>Node</code>, which is restricted to binary tree only.
     *
     * @param onlyTopology  if true, only print topology
     * @param printInternalNodes  if true, print internal nodes
     * @return no ; in the end
     */
    public String toNewick(Node node, boolean onlyTopology, boolean printInternalNodes) {
        final StringBuilder buf = new StringBuilder();

        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            if (i == 0) {
                buf.append("(");
                buf.append(toNewick(child, onlyTopology, printInternalNodes));
            } else {
                buf.append(',');
                buf.append(toNewick(child, onlyTopology, printInternalNodes));
            }
            // print label
            if (child.isLeaf() || printInternalNodes) {
                if (child.getID() == null) {
                    buf.append(child.getNr());
                } else {
                    buf.append(child.getID());
                }
            }
            if (!onlyTopology) {
                buf.append(child.getNewickMetaData());
                buf.append(":").append(child.getLength());
            }
            // close "("
            if (i == children.size() -1) {
                buf.append(")");
            }
        }
        return buf.toString();
    }

    public String toNewick() {
        return this.toNewick(getRoot(), false, false) + ";";
    }

    public boolean isRoot(Node node) {
        return (node == getRoot());
    }


    //++++++++ the sum of squared distances ++++++++

    /**
     * Calculate the sum of squared distances of branch length (distance)
     * given a tree or subtree <code>node</code>.
     *
     * @return the sum of squared residuals
     */
    public double getSumOfSquaredDistance() {
        return this.getSumOfSquaredDistances(getRoot());
    }

    /**
     * Post order traversal to calculate the sum of squared distances of branch length (distance)
     * given a tree or subtree <code>node</code>.
     *
     * @param node the root of the given tree or subtree
     * @return the sum of squared residuals
     */
    private double getSumOfSquaredDistances(Node node) {
        double ss = 0;
        for (Node child : node.getChildren()) {
            double d = node.getHeight() - child.getHeight();
            ss += getSumOfSquaredDistances(child) + d * d;
//            System.out.println(child.getNr() + " : " + ss + " , " + d);
        }
        return ss;
    }

    /**
     *
     * @param rootNode
     * @return
     */
    public FlexibleTree getMinSSDTree(final Node rootNode) {
        FlexibleTree tree = new FlexibleTree(rootNode);
        double minSSD = tree.getSumOfSquaredDistance();
        System.out.println("ssd = " + minSSD + ", tree = " + tree.toNewick());
        // all child nodes including this node
        for (Node node : rootNode.getAllChildNodes()) {
            if (!node.isRoot() && !node.getParent().isRoot()) {
                tree.changeRootTo(node, 0.5);
                double ssd = tree.getSumOfSquaredDistance();
                System.out.println("ssd = " + ssd + ", tree = " + tree.toNewick());
                if (ssd < minSSD) {
                    minSSD = ssd;
                }
            }
        }
        System.out.println("min sum of squared distances = " + minSSD);
        return tree;
    }


    public FlexibleTree getMinSSDTree() {
        return this.getMinSSDTree(getRoot());
    }
}
