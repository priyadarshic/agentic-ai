package output_code;

public class DoublyLinkedList {

    // Node: Represents a single element in the Doubly Linked List.
    // Each node contains data and pointers to the next and previous nodes.
    private static class Node {
        int data;           // The data stored in this node (e.g., an integer).
        Node next;          // A reference to the next Node in the list.  Null if this is the last node.
        Node prev;          // A reference to the previous Node in the list. Null if this is the first node.

        // Node Constructor: Initializes a new node with the given data.
        Node(int data) {
            this.data = data; // Assign the provided data to the node's data field.
            this.next = null; // Initially, the next node is set to null.
            this.prev = null; // Initially, the previous node is set to null.
        }
    }

    private Node head;      // 'head' points to the first Node in the list. Null if the list is empty.
    private Node tail;      // 'tail' points to the last Node in the list. Null if the list is empty.
    private int size;       // 'size' stores the number of nodes currently in the list.

    // DoublyLinkedList Constructor: Creates an empty Doubly Linked List.
    public DoublyLinkedList() {
        this.head = null; // When the list is created, there are no nodes, so 'head' is null.
        this.tail = null; // Similarly, 'tail' is also null.
        this.size = 0;  // The list starts with a size of 0.
    }

    // add(int data): Adds a new node containing the given data to the end of the list.
    public void add(int data) {
        // 1. Create a new Node
        Node newNode = new Node(data); // A new node is created to hold the data.

        // 2. Handle the case where the list is empty
        if (head == null) {
            // If the list is empty (head is null), the new node becomes both the head and tail.
            head = newNode;
            tail = newNode;
        } else {
            // 3. Add to the end of the existing list
            tail.next = newNode;    // The 'next' pointer of the current tail is set to point to the new node.
            newNode.prev = tail;    // The 'prev' pointer of the new node is set to point to the current tail.
            tail = newNode;         // The 'tail' is updated to be the new node, as it is now the last node.
        }
        // 4. Increment the size of the list
        size++; // Increment the list size because a node was added
    }

    // insert(int data, int index): Inserts a new node with the given data at the specified index.
    public void insert(int data, int index) {
        // 1. Validate the index
        if (index < 0 || index > size) {
            // If the index is out of bounds (less than 0 or greater than the current size),
            // an IndexOutOfBoundsException is thrown.
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        // 2. Handle insertion at the end (equivalent to add)
        if (index == size) {
            add(data); // If the index is equal to the size, it means we want to add to the end,
            return;     // so we can reuse the 'add' method and then return.
        }

        // 3. Create the new node
        Node newNode = new Node(data); // Create a new node to hold the data to be inserted.

        // 4. Handle insertion at the beginning
        if (index == 0) {
            // If the index is 0, we are inserting at the beginning.
            newNode.next = head;   // The new node's 'next' pointer points to the current head.
            head.prev = newNode;   // The current head's 'prev' pointer points to the new node.
            head = newNode;        // The head is updated to the new node.
        } else {
            // 5. Insert in the middle
            Node current = head; // Start from the head of the list.

            // Traverse to the node at the specified index
            for (int i = 0; i < index; i++) {
                current = current.next; // Move 'current' to the next node 'index' times.
            }

            // Adjust pointers to insert the new node
            newNode.next = current;       // New node's next points to current
            newNode.prev = current.prev;   // New node's prev points to current's previous
            current.prev.next = newNode;  // The node before current now points to the new node
            current.prev = newNode;       // Current's previous now points to the new node
        }
        // 6. Increment the size
        size++; // Increase the size of the list because a node has been added.
    }

    // remove(int index): Removes the node at the specified index.
    public void remove(int index) {
        // 1. Validate the index
        if (index < 0 || index >= size) {
            // If the index is out of bounds, throw an exception.
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        // 2. Handle removal of the first node
        if (index == 0) {
            // If the index is 0, we are removing the head.
            head = head.next; // The head is moved to the next node.
            if (head != null) {
                head.prev = null; // The new head's 'prev' pointer is set to null because it's now the first node.
            } else {
                tail = null; // If the list becomes empty after removing the head, tail is also set to null.
            }

        } else if (index == size - 1) {
            // 3. Handle removal of the last node
            tail = tail.prev; // Tail is moved to the previous node.
            tail.next = null;  // The new tail's 'next' pointer is set to null because it's now the last node.
        }
        else {
            // 4. Remove from the middle
            Node current = head; // Start from the head.

            // Traverse to the node at the specified index
            for (int i = 0; i < index; i++) {
                current = current.next; // Move 'current' to the next node 'index' times.
            }

            // Adjust pointers to remove the node
            current.prev.next = current.next; // The node before 'current' now points to the node after 'current'.
            current.next.prev = current.prev; // The node after 'current' now points to the node before 'current'.
        }
        // 5. Decrement the size
        size--; // Decrease the size of the list because a node has been removed.
    }

    // get(int index): Returns the data stored in the node at the specified index.
    public int get(int index) {
        // 1. Validate the index
        if (index < 0 || index >= size) {
            // If the index is out of bounds, throw an exception.
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        // 2. Traverse to the node at the specified index
        Node current = head; // Start from the head.
        for (int i = 0; i < index; i++) {
            current = current.next; // Move 'current' to the next node 'index' times.
        }

        // 3. Return the data
        return current.data; // Return the data stored in the node at the specified index.
    }

    // size(): Returns the number of nodes in the list.
    public int size() {
        return size; // Return the current size of the list.
    }

    // isEmpty(): Checks if the list is empty.
    public boolean isEmpty() {
        return size == 0; // If the size is 0, the list is empty, return true. Otherwise, return false.
    }

    // printListForward(): Prints the list from head to tail.
    public void printListForward() {
        Node current = head; // Start from the head.
        System.out.print("List (Forward): "); // Indicate the direction of printing.

        // Traverse the list and print each node's data
        while (current != null) {
            System.out.print(current.data + " "); // Print the data of the current node.
            current = current.next; // Move to the next node.
        }
        System.out.println(); // Print a newline character to end the output.
    }

    // printListBackward(): Prints the list from tail to head.
    public void printListBackward() {
        Node current = tail; // Start from the tail.
        System.out.print("List (Backward): "); // Indicate the direction of printing.

        // Traverse the list backward and print each node's data
        while (current != null) {
            System.out.print(current.data + " "); // Print the data of the current node.
            current = current.prev; // Move to the previous node.
        }
        System.out.println(); // Print a newline character to end the output.
    }

    // Main method for testing the DoublyLinkedList
    public static void main(String[] args) {
        // Create a new DoublyLinkedList object
        DoublyLinkedList list = new DoublyLinkedList();

        // Add some elements to the list
        list.add(10);
        list.add(20);
        list.add(30);
        list.add(67);
        list.add(27);
        list.add(78);
        list.add(93);

        // Print the list in both forward and backward directions
        list.printListForward();   // Output: List (Forward): 10 20 30
        list.printListBackward();  // Output: List (Backward): 30 20 10

        // Insert an element at a specific index
        list.insert(404, 4);
        list.printListForward();   // Output: List (Forward): 10 20 25 30

        // Remove an element from a specific index
        list.remove(4);
        list.printListForward();   // Output: List (Forward): 10 25 30

        // Get an element at a specific index and print it
        System.out.println("Element at index 1: " + list.get(1)); // Output: Element at index 1: 25

        // Get the size of the list and print it
        System.out.println("Size of the list: " + list.size());   // Output: Size of the list: 3

        // Check if the list is empty and print the result
        System.out.println("Is the list empty? " + list.isEmpty()); // Output: Is the list empty? false
    }
}