package com.github.wohatel.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** * ForkTree - Multi-forked tree based on list simulation
 * (supports obtaining parent node and child node indexes)
 *
 */
@Data
public class ForkTree<T, N> {

    /**     
     * The list of nodes in the tree,
     * in hierarchical order, is non-repeatable and non-empty
     */
    private List<N> nodes;

    /**     
     * Number of forks in the
     * tree (default 2-forked tree)
     */
    private int treeFork = 2;

    /**     
     * content
     */
    private T data;

    /**     
     * Get the index of the serial child node of a node
     *
     * @param netNodeIndex Parent index
     * @param serial       Children (from 0)
     * @return If the child node index does not exist, it returns -1
     */
    public int getChildIndex(int netNodeIndex, int serial) {
        if (isNodesEmpty()) {
            throw new IllegalStateException("nodes list is empty");
        }
        if (serial < 0 || serial >= treeFork) {
            return -1;
        }
        int childIndex = treeFork * netNodeIndex + (serial + 1);
        return (childIndex < nodes.size()) ? childIndex : -1;
    }

    /**     
     * Get the parent index of a node
     *
     * @param netNodeIndex Current node index
     * @return The parent node index returns -1 if it is the root node or an illegal index
     */
    public int getParentIndex(int netNodeIndex) {
        if (isNodesEmpty()) {
            throw new IllegalStateException("nodes list is empty");
        }
        if (netNodeIndex <= 0 || netNodeIndex >= nodes.size()) {
            return -1;
        }
        return (netNodeIndex - 1) / treeFork;
    }

    /**     
     * Get all the indexes of the lineal
     * parent of a node all the way to the root node (not including itself)
     *
     * @param netNodeIndex Current node index
     * @return Parent index list (from near to far)
     */
    public List<Integer> getAllParents(int netNodeIndex) {
        if (isNodesEmpty()) {
            throw new IllegalStateException("nodes list is empty");
        }
        List<Integer> parents = new ArrayList<>();
        int current = netNodeIndex;
        while (true) {
            int parent = getParentIndex(current);
            if (parent == -1) {
                break;
            }
            parents.add(parent);
            current = parent;
        }
        return parents;
    }

    /**     
     * Get all descendant node indexes (direct and indirect) of a node
     *
     * @param netNodeIndex Current node index
     * @return List of descendant node indexes
     */
    public List<Integer> getAllChildren(int netNodeIndex) {
        if (isNodesEmpty()) {
            throw new IllegalStateException("nodes list is empty");
        }
        List<Integer> children = new ArrayList<>();
        collectChildren(netNodeIndex, children);
        return children;
    }

    /**     
     * Recursively collect descendant nodes
     */
    private void collectChildren(int netNodeIndex, List<Integer> result) {
        for (int serial = 0; serial < treeFork; serial++) {
            int childIndex = getChildIndex(netNodeIndex, serial);
            if (childIndex != -1) {
                result.add(childIndex);
                collectChildren(childIndex, result);
            }
        }
    }

    private boolean isNodesEmpty() {
        return nodes == null || nodes.isEmpty();
    }
}