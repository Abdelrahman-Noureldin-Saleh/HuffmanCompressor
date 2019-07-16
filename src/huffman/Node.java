/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package huffman;

/**
 *
 * @author Abd El Rahman
 */
public class Node implements Comparable{
    
    byte letter;
    int frequency;
    Node left;
    Node right;


    @Override
    public int compareTo(Object o) {
        return this.frequency - ((Node)o).frequency;
    }
    
    
    @Override
    public String toString() {
        return "Node{" + "letter=" + (char)letter + ", frequency=" + frequency + '}';
    }
    
    
    
    
    
}
