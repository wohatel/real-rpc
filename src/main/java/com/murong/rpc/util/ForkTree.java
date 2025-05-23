package com.murong.rpc.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ForkTree - 基于列表模拟的多叉树（支持获取父节点、子节点索引）
 *
 * @param <T> 广播的数据类型
 * @param <N> 节点绑定的数据类型
 */
@Data
public class ForkTree<T, N> {

    /**
     * 树的节点列表，按层次顺序排列，节点不可重复且非空
     */
    private List<N> nodes;

    /**
     * 树的分叉数（默认2叉树）
     */
    private int treeFork = 2;

    /**
     * 广播的内容
     */
    private T data;

    /**
     * 获取某节点的第serial个孩子节点的索引
     *
     * @param netNodeIndex 父节点索引
     * @param serial       第几个孩子（从0开始）
     * @return 子节点索引，若不存在返回-1
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
     * 获取某节点的父节点索引
     *
     * @param netNodeIndex 当前节点索引
     * @return 父节点索引，若为根节点或非法索引则返回-1
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
     * 获取某节点的所有直系父节点索引，一直到根节点（不包含自己）
     *
     * @param netNodeIndex 当前节点索引
     * @return 父节点索引列表（从近到远）
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
     * 获取某节点的所有子孙节点索引（直系和间接）
     *
     * @param netNodeIndex 当前节点索引
     * @return 子孙节点索引列表
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
     * 递归收集子孙节点
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

    /**
     * 判断nodes列表是否为空
     */
    private boolean isNodesEmpty() {
        return nodes == null || nodes.isEmpty();
    }
}