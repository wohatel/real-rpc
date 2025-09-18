package com.github.wohatel.util;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * description
 *
 * @author yaochuang 2025/09/12 15:21
 */
@Getter
@Setter
public class LinkedNode<K, T> implements Iterable<LinkedNode<K, T>> {
    private K key;
    private T value;

    private LinkedNode<K, T> next;
    private LinkedNode<K, T> pre;


    private LinkedNode(K key, T value) {
        this.key = key;
        this.value = value;
    }


    public static <K, T> LinkedNode<K, T> build(K mark, T value) {
        return new LinkedNode<>(mark, value);
    }

    public static <K, T> LinkedNode<K, T> build(T value) {
        return new LinkedNode<>(null, value);
    }


    public LinkedNode<K, T> findFirst(String key) {
        LinkedNode<K, T> first = findFirst();
        if (key == null) {
            return first;
        }
        Iterator<LinkedNode<K, T>> iterator = first.iterator();
        while (iterator.hasNext()) {
            LinkedNode<K, T> next1 = iterator.next();
            if (key.equals(next1.getKey())) {
                return next1;
            }
        }
        return null;
    }

    public LinkedNode<K, T> findFirst() {
        LinkedNode<K, T> current = this;
        while (current.pre != null) {
            current = current.pre;
        }
        return current;
    }

    /**
     * 当前节点添加下个节点
     *
     * @param next 要添加的下个节点
     */
    public LinkedNode<K, T> addNext(LinkedNode<K, T> next) {
        verifyNode(next);
        this.next = next;
        next.pre = this;

        return next;
    }


    /**
     * 判断是否包含node
     *
     * @param node 判断是否包含node
     * @return boolean 是否包含
     */
    public boolean containsNode(LinkedNode<K, T> node) {
        if (node == null) {
            return false;
        }
        // 回溯到头节点
        LinkedNode<K, T> current = this;
        while (current.pre != null) {
            current = current.pre;
        }

        // 从头节点向下遍历
        while (current != null) {
            if (current == node) {  // 比较节点对象本身
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * 校验节点
     *
     * @param node 节点
     */
    private void verifyNode(LinkedNode<K, T> node) {
        if (node == null) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "node can't be null");
        }
        if (containsNode(node)) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "node has already exists");
        }
    }

    /**
     * 是否有下游节点
     *
     * @return boolean
     */
    public boolean hasNext() {
        return this.next != null;
    }

    /**
     * 获取头节点
     */
    public LinkedNode<K, T> findHead() {
        LinkedNode<K, T> current = this;
        while (current.pre != null) {
            current = current.pre;
        }
        return current;
    }

    /**
     * 获取尾节点
     */
    public LinkedNode<K, T> findLast() {
        LinkedNode<K, T> current = this;
        while (current.next != null) {
            current = current.next;
        }
        return current;
    }

    /**
     * 在链表尾部追加节点
     */
    public LinkedNode<K, T> addLast(LinkedNode<K, T> node) {
        verifyNode(node);
        LinkedNode<K, T> last = findLast();
        last.next = node;
        node.pre = last;

        return node;
    }

    /**
     * 在 target 节点之前插入 node
     */
    public LinkedNode<K, T> addBefore(LinkedNode<K, T> node) {
        verifyNode(node);
        LinkedNode<K, T> prev = this.getPre();
        if (prev != null) {
            prev.setNext(node);
        }
        node.setPre(prev);
        node.setNext(this);
        this.setPre(node);

        return node;
    }


    /**
     * 删除节点,不存在不会报错
     */
    public void remove(LinkedNode<K, T> node) {
        if (!containsNode(node)) {
            return;
        }
        LinkedNode<K, T> prev = node.getPre();
        LinkedNode<K, T> next = node.getNext();
        if (prev != null) {
            prev.setNext(next);
        }
        if (next != null) {
            next.setPre(prev);
        }
    }

    /**
     * 从当前节点开始迭代
     */
    @Override
    public Iterator<LinkedNode<K, T>> iterator() {
        return new Iterator<>() {
            private LinkedNode<K, T> current = LinkedNode.this;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public LinkedNode<K, T> next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                LinkedNode<K, T> temp = current;
                current = current.getNext();
                return temp;
            }
        };
    }

}
