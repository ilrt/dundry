/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.model;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class Tree<T> {
    
    private final List<Tree<T>> children;
    private final T node;
    
    public Tree(T leaf) {
        this.node = leaf;
        this.children = Collections.EMPTY_LIST;
    }
    
    public Tree(T node, List<Tree<T>> children) {
        this.node = node;
        this.children = children;
    }
    
    public T getNode() { return node; }
    
    public List<Tree<T>> getChildren() { return children; }
    
    @Override
    public String toString() {
        return buildString("", new StringBuilder()).toString();
    }
    
    private StringBuilder buildString(String indent, StringBuilder in) {
        in.append(indent).append("[").append(node.toString()).append("]\n");
        for (Tree<T> c: children) c.buildString(indent + " ", in);
        return in;
    }
}
