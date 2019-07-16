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
class TableEntry implements Comparable{
    
    byte symbol;
    int codeLength;
    String code;
    String canonicalCode;
    int frequency;

    @Override
    public int compareTo(Object o) {
        return this.codeLength - ((TableEntry)o).codeLength;
    }

    @Override
    public String toString() {
        return "TableEntry{" + "symbol=" + symbol + ", code=" + code + '}';
    }
    
    
    
    
    
    
}
