package output_code;

/**
 * This program demonstrates how to invert a binary tree.
 * Inverting a binary tree means swapping the left and right children of each node.
 *
 * The program defines a Node class to represent a node in the binary tree.
 * It also provides a method to invert the tree recursively.
 *
 * Example:
 *
 *      Original Tree:
 *
 *          1
 *         / \
 *        2   3
 *       / \
 *      4   5
 *
 *      Inverted Tree:
 *
 *          1
 *         / \
 *        3   2
 *           / \
 *          5   4
 */
public class InvertBinaryTree {

    // Node class representing a node in the binary tree
    class Node {
        int data;
        Node left, right;

        public Node(int data) {
            this.data = data;
            left = right = null;
        }
    }

    /**
     * Inverts the given binary tree.
     *
     * @param root The root of the binary tree to invert.
     * @return The root of the inverted binary tree.
     */
    public Node invertTree(Node root) {
        // Base case: If the root is null, there's nothing to invert.
        if (root == null) {
            return null; // Return null if the current node is null
        }

        // Recursively invert the left subtree
        Node left = invertTree(root.left);
        // Recursively invert the right subtree
        Node right = invertTree(root.right);

        // Swap the left and right children of the current node.
        root.left = right; // Assign the previously right subtree to the left of current node
        root.right = left; // Assign the previously left subtree to the right of current node

        // Return the root of the inverted subtree.
        return root; // Return the current node (root of the inverted subtree)
    }

    /**
     * Builds a binary tree from an array of integers.
     * Null values in the array represent missing nodes.
     *
     * @param arr The array of integers representing the tree.
     * @param index The current index in the array.
     * @return The root of the constructed binary tree.
     */
    public Node buildTreeFromArray(Integer[] arr, int index) {
        if (index >= arr.length || arr[index] == null) {
            return null;
        }

        Node root = new Node(arr[index]);
        root.left = buildTreeFromArray(arr, 2 * index + 1);
        root.right = buildTreeFromArray(arr, 2 * index + 2);

        return root;
    }


    public static void main(String[] args) {
        InvertBinaryTree treeInverter = new InvertBinaryTree();

        // Test case 1: Basic tree
        Integer[] arr1 = {1, 2, 3, 4, 5, 6, 7};
        Node root1 = treeInverter.buildTreeFromArray(arr1, 0);
        System.out.println("Original Tree 1:");
        printTree(root1, 0);
        Node invertedRoot1 = treeInverter.invertTree(root1);
        System.out.println("\nInverted Tree 1:");
        printTree(invertedRoot1, 0);
        System.out.println("\n");

        // Test case 2: Skewed tree (left-skewed)
        Integer[] arr2 = {1, 2, null, 3, null, null, null};
        Node root2 = treeInverter.buildTreeFromArray(arr2, 0);
        System.out.println("Original Tree 2 (Left Skewed):");
        printTree(root2, 0);
        Node invertedRoot2 = treeInverter.invertTree(root2);
        System.out.println("\nInverted Tree 2 (Right Skewed):");
        printTree(invertedRoot2, 0);
        System.out.println("\n");

        // Test case 3: Skewed tree (right-skewed)
        Integer[] arr3 = {1, null, 2, null, null, null, 3};
        Node root3 = treeInverter.buildTreeFromArray(arr3, 0);
        System.out.println("Original Tree 3 (Right Skewed):");
        printTree(root3, 0);
        Node invertedRoot3 = treeInverter.invertTree(root3);
        System.out.println("\nInverted Tree 3 (Left Skewed):");
        printTree(invertedRoot3, 0);
        System.out.println("\n");

        // Test case 4: Empty tree
        Integer[] arr4 = {};
        Node root4 = treeInverter.buildTreeFromArray(arr4, 0);
        System.out.println("Original Tree 4 (Empty):");
        printTree(root4, 0);
        Node invertedRoot4 = treeInverter.invertTree(root4);
        System.out.println("\nInverted Tree 4 (Empty):");
        printTree(invertedRoot4, 0);
        System.out.println("\n");

        // Test case 5: Single node tree
        Integer[] arr5 = {5};
        Node root5 = treeInverter.buildTreeFromArray(arr5, 0);
        System.out.println("Original Tree 5 (Single Node):");
        printTree(root5, 0);
        Node invertedRoot5 = treeInverter.invertTree(root5);
        System.out.println("\nInverted Tree 5 (Single Node):");
        printTree(invertedRoot5, 0);
        System.out.println("\n");
    }

    /**
     * Prints the tree in a visual format.
     *
     * @param root The root of the tree.
     * @param level The level of the current node (used for indentation).
     */
    public static void printTree(Node root, int level) {
        if (root == null) {
            return;
        }

        printTree(root.right, level + 1);

        for (int i = 0; i < level; i++) {
            System.out.print("    ");
        }
        System.out.println(root.data);

        printTree(root.left, level + 1);
    }
}