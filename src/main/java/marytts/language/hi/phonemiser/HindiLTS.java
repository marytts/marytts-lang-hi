/**
 * Copyright 2002-2008 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.language.hi.phonemiser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class HindiLTS {

    private HashMap<String, String> UTF8toPhoneSymbols;
    private HashMap<String, String> UTF8toPhoneTypes;
    private ArrayList<String> listPhoneSym;
    private ArrayList<String> listPhoneTypes;
    private ArrayList<String> listConTypes;
    
    
    private ArrayList<String> utf8CharList;
    
    /**
     * TeluguLTS constructor
     * @param utf8toit3map
     * @throws IOException
     */
    public HindiLTS(InputStream utf8toit3mapStream) throws IOException{
        this.loadPhoneSymbolsAndTypes(utf8toit3mapStream);
    }
  
    
    public String phonemise(String word) throws IOException
    {
    	word = word.trim();
        utf8CharList = readUTF8String(word);
        listPhoneSym = new ArrayList<String>();
        listPhoneTypes = new ArrayList<String>();
        listConTypes = new ArrayList<String>();
        
        Iterator<String> listrun = utf8CharList.iterator();
        while(listrun.hasNext())
        {
            String utf8Char    = listrun.next();
            String phoneSymbol = UTF8toPhoneSymbols.get(utf8Char);
            String phoneType   = UTF8toPhoneTypes.get(utf8Char);
            if(phoneSymbol == null) phoneSymbol = getAsciiChar(utf8Char);
            if(phoneType == null) phoneType = "#";
            listPhoneSym.add(phoneSymbol);
            listPhoneTypes.add(phoneType);
            if ("CON".endsWith(phoneType)) {
            	listConTypes.add("U");
            	//System.out.println(utf8Char+" "+phoneSymbol+" "+phoneType+ " U");
            } else {
            	listConTypes.add("#");
            	//System.out.println(utf8Char+" "+phoneSymbol+" "+phoneType+" #");
            }
            
        }
        
        removeUnknownSymbols(); 
        //printArrayList(listPhoneSym);
        //printArrayList(listPhoneTypes);
        //printArrayList(listConTypes);
        schwaHandler();
        //printArrayList(listPhoneSym);
        removeHal();
        syllabify();
        putStressMark();
        
        return getStringfromArrayList(listPhoneSym);
    }
    
    /**
     * Add stress mark on first syllable
     * @return
     */
    private ArrayList<String> putStressMark() {
        listPhoneSym.add(0,"'");
        return listPhoneSym;
    }

    /**
     * Add syllable symbols at proper places
     */
    private void syllabify() {
    
        for(int i=0; i<listPhoneTypes.size(); i++){
            if(isVowel(i)){
                boolean isVowelLater  = isVowelLater(i);
                boolean isNextSemiCon = isNextSemiConsonant(i);
                if(isVowelLater){
                    if( isNextSemiCon ) {
                        listPhoneSym.add(i+2,"-");
                        listPhoneTypes.add(i+2,"SYM");
                    }
                    else {
                        listPhoneSym.add(i+1,"-");
                        listPhoneTypes.add(i+1,"SYM");
                    }
                }
            }
        }
    }
    
    /**
     * Check whether the character is Vowel or not
     * @param pos
     * @return
     */
    private boolean isVowel(int pos){
        if(listPhoneTypes.get(pos).equals("VOW")) {
                return true;
        }
        return false;    
    }
    
    /**
     * Check whether the word has vowels after given position
     * @param pos
     * @return
     */
    private boolean isVowelLater(int pos){
        for(int i=(pos+1); i<listPhoneTypes.size(); i++){
            if(listPhoneTypes.get(i).equals("VOW")) {
                return true;
            }
        }
        return false;    
    }
    
    /**
     * check next position is semiconsonant 
     * @param pos
     * @return
     */
    private boolean isNextSemiConsonant(int pos){
        if((pos+1) >= listPhoneSym.size()) return false;
        if(listPhoneSym.get(pos+1).equals("n:") || listPhoneSym.get(pos+1).equals("a:")) {
                return true;
        }
        return false;    
    }
    
    /**
     * Get a string from arraylist
     * @param lPhoneSym
     * @param lPhoneTypes
     * @return
     */
    private String getStringfromArrayList(ArrayList<String> aList) {
        Iterator<String> listrun = aList.iterator();
        StringBuilder result = new StringBuilder();
        while(listrun.hasNext())
        {
            result.append(listrun.next());
        }
        return result.toString();
    }

    /**
     * Hex-decimal representation for a given string
     * @param ch
     * @return
     */
    private String toHex4(int ch) 
    {
        String hex = Integer.toHexString(ch).toUpperCase();
        switch (hex.length()) {
            case 3  : return "0" + hex;
            case 2  : return "00" + hex;
            case 1  : return "000" + hex;
            default : return hex;
        }
    }

    
    private void loadPhoneSymbolsAndTypes(InputStream inStream) throws IOException
    {
        String line;
        BufferedReader bfr = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
        UTF8toPhoneSymbols = new HashMap<String, String>();
        UTF8toPhoneTypes = new HashMap<String, String>();
        while ( (line = bfr.readLine()) != null )
        {
            String[] words = line.split("\\|");
            UTF8toPhoneSymbols.put(words[0],words[1]);
            UTF8toPhoneTypes.put(words[0],words[2]);
        }
        bfr.close();
    }
        
   
    
    /**
     * verification for 'KA' varg based on unicode chart
     * @param uniCodeHex
     * @return
     */
    private boolean isBelongs2KAVarga(String uniCodeHex) {
        int unicode = hexString2Int(uniCodeHex);
        int minFVChart = hexString2Int("0915"); 
        int maxFVChart = hexString2Int("0919");
        
        if ( unicode >= minFVChart && unicode <= maxFVChart ) {
            return true;
        }
        return false;
    }
    
    /**
     * verification for 'CHA' varg based on unicode chart
     * @param uniCodeHex
     * @return
     */
    private boolean isBelongs2CHAVarga(String uniCodeHex) {
        int unicode = hexString2Int(uniCodeHex);
        int minFVChart = hexString2Int("091A"); 
        int maxFVChart = hexString2Int("091E");
        
        if ( unicode >= minFVChart && unicode <= maxFVChart ) {
            return true;
        }
        return false;
    }
    
    /**
     * verification for 'TA' varg based on unicode chart
     * @param uniCodeHex
     * @return
     */
    private boolean isBelongs2TAVarga(String uniCodeHex) {
        int unicode = hexString2Int(uniCodeHex);
        int minFVChart = hexString2Int("091F"); 
        int maxFVChart = hexString2Int("0923");
        
        if ( unicode >= minFVChart && unicode <= maxFVChart ) {
            return true;
        }
        return false;
    }
    
    /**
     * verification for 'THA' varg based on unicode chart
     * @param uniCodeHex
     * @return
     */
    private boolean isBelongs2THAVarga(String uniCodeHex) {
        int unicode = hexString2Int(uniCodeHex);
        int minFVChart = hexString2Int("0924"); 
        int maxFVChart = hexString2Int("0929");
        
        if ( unicode >= minFVChart && unicode <= maxFVChart ) {
            return true;
        }
        return false;
    }
    
    /**
     * verification for 'PA' varg based on unicode chart
     * @param uniCodeHex
     * @return
     */
    private boolean isBelongs2PAVarga(String uniCodeHex) {
        int unicode = hexString2Int(uniCodeHex);
        int minFVChart = hexString2Int("092A"); 
        int maxFVChart = hexString2Int("092E");
        
        if ( unicode >= minFVChart && unicode <= maxFVChart ) {
            return true;
        }
        return false;
    }
    
    /**
     * verification for 'YA' varg based on unicode chart
     * @param uniCodeHex
     * @return
     */
    private boolean isBelongs2YAVarga(String uniCodeHex) {
        int unicode = hexString2Int(uniCodeHex);
        int minFVChart = hexString2Int("092F"); 
        int maxFVChart = hexString2Int("0939");
        
        if ( unicode >= minFVChart && unicode <= maxFVChart ) {
            return true;
        }
        return false;
    }
    
    public ArrayList<String> readUTF8String(String word) throws IOException
    {
        CharBuffer cbuf = CharBuffer.wrap(word);
        ArrayList<String> utf8CharList = new ArrayList<String>();
        for(int i=0; i<cbuf.length(); i++){
            char ch = cbuf.get(i);
            utf8CharList.add(toHex4((int)ch));
        }
        return utf8CharList;
    }
    
    public ArrayList<String> readUTF8File(String filename) throws IOException
    {
        int ch;
        ArrayList<String> utf8CharList = new ArrayList<String>();
        InputStreamReader ins = new InputStreamReader(new FileInputStream(filename), "UTF8");
        while((ch = ins.read()) >= 0){
            utf8CharList.add(toHex4(ch));
        }
        return utf8CharList;
    }
    
    private void printData(String filename) throws IOException
    {
        ArrayList<String> utf8CharList = readUTF8File(filename);
        
        Iterator<String> listrun = utf8CharList.iterator();
        while(listrun.hasNext())
        {
            String utf8Char    = listrun.next();
            String phoneSymbol = UTF8toPhoneSymbols.get(utf8Char);
            String phoneType   = UTF8toPhoneTypes.get(utf8Char);
            if(phoneSymbol == null) phoneSymbol = "SPACE";
            if(phoneType == null) phoneType = "#";
            System.out.println(utf8Char+" "+phoneSymbol+" "+phoneType);
        }
    }
    
    
    
    public void makeProperIt3(String filename) throws IOException
    {
        Scanner sc = new Scanner(new File(filename));
        
        while (sc.hasNextLine()) {
        	String line = sc.nextLine().trim();
        	String[] words = line.split("\\s+");
        	for ( int i=0; i<words.length; i++ ) {
        		System.out.println(words[i]+" --> "+this.phonemise(words[i].trim()));
        	}
        }  
    }
    
 
    /**
     * Remove Halanth from telugu characters 
     * @param lPhoneSym
     * @param lPhoneTypes
     * @return
     */
    private ArrayList<String> removeHal(ArrayList<String> lPhoneSym,
            ArrayList<String> lPhoneTypes) {
       
        for(int i=0; i<lPhoneTypes.size(); i++){
            if(lPhoneTypes.get(i).equals("HLT")){
                lPhoneTypes.remove(i);
                lPhoneSym.remove(i);
                i--;
            }
        }
        return lPhoneSym;
    }
    
    /**
     * Remove Halanth from telugu characters
     */
    private void removeHal() {
       
        for(int i=0; i<listPhoneTypes.size(); i++){
            if(listPhoneTypes.get(i).equals("HLT")){
                listPhoneTypes.remove(i);
                listPhoneSym.remove(i);
                i--;
            }
        }
    }
    
    /**
     * Remove unknown symbols
     */
    private void removeUnknownSymbols() {
       
        for(int i=0; i<listPhoneTypes.size(); i++){
            if(listPhoneTypes.get(i).equals("#")){
                listPhoneTypes.remove(i);
                listPhoneSym.remove(i);
                i--;
            }
        }
    }


    /**
     * get ascii values for utf8 characters
     * @param utf8Char
     * @return
     */
    private String getAsciiChar(String utf8Char) {
        int intValue = Integer.parseInt(utf8Char, 16);
        char dec = (char)intValue;
        return Character.toString(dec);
    }
    
    
    /**
     * Schwa handler 
     */
    private void RuleSetZero() {
        
        String prevType, nextType;
        String prevPhone, nextPhone;
        String prevUchar, nextUchar;
        boolean isFinalCharacter = false;
        
        for(int i=0; i<listPhoneTypes.size(); i++){
            
            //if(listPhoneTypes.get(i) == null) continue;
            //if(listPhoneTypes.get(i+1) == null) ;
            prevType  = listPhoneTypes.get(i);
            prevPhone = this.listPhoneSym.get(i);
            prevUchar = this.utf8CharList.get(i);
            
            if ( (i+1) < listPhoneTypes.size() ) {
                nextType = listPhoneTypes.get(i+1);
                nextPhone = this.listPhoneSym.get(i+1);
                nextUchar = this.utf8CharList.get(i+1);
            } else {
                nextType = listPhoneTypes.get(i);
                nextPhone = this.listPhoneSym.get(i);
                nextUchar = this.utf8CharList.get(i);
                isFinalCharacter = true;
            }
            
            // Bindu handling
            if (prevUchar.equals("0902")) {
                if (isFinalCharacter == true) {
                    listPhoneTypes.set(i, "CON");
                    listPhoneSym.set(i, "ng~");
                    utf8CharList.set(i, "0919");
                    listConTypes.set(i, "U");
                } else if ( this.isBelongs2TAVarga(nextUchar) ) {
                    listPhoneTypes.set(i, "CON");
                    listPhoneSym.set(i, "n");
                    utf8CharList.set(i, "0928");
                    listConTypes.set(i, "U");
                } else if ( this.isBelongs2PAVarga(nextUchar) ) {
                    listPhoneTypes.set(i, "CON");
                    listPhoneSym.set(i, "m");
                    utf8CharList.set(i, "092E");
                    listConTypes.set(i, "U");
                } else if ( this.isBelongs2KAVarga(nextUchar) ) {
                    listPhoneTypes.set(i, "CON");
                    listPhoneSym.set(i, "ng~");
                    utf8CharList.set(i, "0919");
                    listConTypes.set(i, "U");
                }
            }
            
           // if (isFinalCharacter == true) {
           //     break;
           // }
            
            //System.err.println(prevType+" "+prevPhone+" "+prevUchar+" "+nextType+" "+nextPhone+" "+nextUchar);
                
            if ( (prevType.equals("CON") && nextType.equals("VOW")) ) {
            	listConTypes.set(i, "F");
            } else if (prevType.equals("VOW")) {
            	listConTypes.set(i, "F");
            }
        }
    }
    
    private void schwaHandler() {
    	
    	//printSchwaSequence();
    	RuleSetZero();
    	//printSchwaSequence();
    	RuleSetOne();
    	//printSchwaSequence();
    	RuleSetTwo();
    	//printSchwaSequence();
    	RuleSetThree();
    	//printSchwaSequence();
    	RuleSetFour();
    	//printSchwaSequence();
    	RuleSetFive();
    	//printSchwaSequence();
    	RuleSetSix();
    	//printSchwaSequence();
    	RuleSetSeven();
       	//printSchwaSequence();
    	RuleSetEight();
       //	printSchwaSequence();
       	
       	FinalizeRules();
    }
    
    private void FinalizeRules() {
    	 String prevType, nextType;
         String prevPhone, nextPhone;
         String prevUchar, nextUchar;
         String prevCon, nextCon;
         boolean isFinalCharacter = false;
         boolean encounterF = false;
         
         for(int i=0; i<listPhoneTypes.size(); i++){
        	 prevType  = listPhoneTypes.get(i);
             prevPhone = this.listPhoneSym.get(i);
             prevUchar = this.utf8CharList.get(i);
             prevCon = this.listConTypes.get(i);
             
             if ( (i+1) < listPhoneTypes.size() ) {
                 nextType = listPhoneTypes.get(i+1);
                 nextPhone = this.listPhoneSym.get(i+1);
                 nextUchar = this.utf8CharList.get(i+1);
                 nextCon = this.listConTypes.get(i+1);
             } else {
                 nextType = listPhoneTypes.get(i);
                 nextPhone = this.listPhoneSym.get(i);
                 nextUchar = this.utf8CharList.get(i);
                 nextCon = this.listConTypes.get(i);
                 isFinalCharacter = true;
             }
             
             
             if (isFinalCharacter == true) {
                 break;
             }
             
             if ("F".equals(prevCon) && "CON".equals(prevType)) {
            	 if ( !"VOW".equals(nextType) || isFullVowel(nextUchar)) {
            		 listPhoneTypes.add(i+1, "VOW");
                     listPhoneSym.add(i+1, "a");
                     this.utf8CharList.add(i+1,"093D");
                     listConTypes.add(i+1, "#");
            	 }
             }
         }
 	}
    
    private void RuleSetTwo() {
    	 String prevType, nextType;
         String prevPhone, nextPhone;
         String prevUchar, nextUchar;
         String prevCon, nextCon;
         boolean isFinalCharacter = false;
         
         for(int i=0; i<listPhoneTypes.size(); i++){
        	 prevType  = listPhoneTypes.get(i);
             prevPhone = this.listPhoneSym.get(i);
             prevUchar = this.utf8CharList.get(i);
             prevCon = this.listConTypes.get(i);
             
             if ( (i+1) < listPhoneTypes.size() ) {
                 nextType = listPhoneTypes.get(i+1);
                 nextPhone = this.listPhoneSym.get(i+1);
                 nextUchar = this.utf8CharList.get(i+1);
                 nextCon = this.listConTypes.get(i+1);
             } else {
                 nextType = listPhoneTypes.get(i);
                 nextPhone = this.listPhoneSym.get(i);
                 nextUchar = this.utf8CharList.get(i);
                 nextCon = this.listConTypes.get(i);
                 isFinalCharacter = true;
             }
             
             
             if (isFinalCharacter == true) {
                 break;
             }
             //printSchwaSequence();
            
             if ("y".equals(nextPhone)) {
            	  if ("i".equals(prevPhone) || "ii".equals(prevPhone) || "u".equals(prevPhone) || "uu".equals(prevPhone) ){
            		  listConTypes.set(i+1, "F");
            	  } else if ("U".equals(prevCon)) {
            		  listConTypes.set(i+1, "F");
            	  }
             }
         }
	}
    
    private void RuleSetThree() {
   	 String prevType, nextType;
        String prevPhone, nextPhone;
        String prevUchar, nextUchar;
        String prevCon, nextCon;
        boolean isFinalCharacter = false;
        
        for(int i=0; i<listPhoneTypes.size(); i++){
       	 prevType  = listPhoneTypes.get(i);
            prevPhone = this.listPhoneSym.get(i);
            prevUchar = this.utf8CharList.get(i);
            prevCon = this.listConTypes.get(i);
            
            if ( (i+1) < listPhoneTypes.size() ) {
                nextType = listPhoneTypes.get(i+1);
                nextPhone = this.listPhoneSym.get(i+1);
                nextUchar = this.utf8CharList.get(i+1);
                nextCon = this.listConTypes.get(i+1);
            } else {
                nextType = listPhoneTypes.get(i);
                nextPhone = this.listPhoneSym.get(i);
                nextUchar = this.utf8CharList.get(i);
                nextCon = this.listConTypes.get(i);
                isFinalCharacter = true;
            }
            
            
            if (isFinalCharacter == true) {
                break;
            }
            //printSchwaSequence();

            if ("U".equals(nextCon)) {
            	if ("y".equals(nextPhone) || "r".equals(nextPhone) || "l".equals(nextPhone) || "v".equals(nextPhone) ){
            		if ("CON".equals(prevType) && "H".equals(prevCon) ) {
            			listConTypes.set(i+1, "F"); 
            		} else if ("HAL".equals(prevPhone)) {
            			listConTypes.set(i+1, "F");  
            		}

            	}
            }
        }
    }

    private void RuleSetOne() {
   	 String prevType, nextType;
        String prevPhone, nextPhone;
        String prevUchar, nextUchar;
        String prevCon, nextCon;
        boolean isFinalCharacter = false;
        
        for(int i=0; i<listPhoneTypes.size(); i++){
       	 prevType  = listPhoneTypes.get(i);
            prevPhone = this.listPhoneSym.get(i);
            prevUchar = this.utf8CharList.get(i);
            prevCon = this.listConTypes.get(i);
            
            if ( (i+1) < listPhoneTypes.size() ) {
                nextType = listPhoneTypes.get(i+1);
                nextPhone = this.listPhoneSym.get(i+1);
                nextUchar = this.utf8CharList.get(i+1);
                nextCon = this.listConTypes.get(i+1);
            } else {
                nextType = listPhoneTypes.get(i);
                nextPhone = this.listPhoneSym.get(i);
                nextUchar = this.utf8CharList.get(i);
                nextCon = this.listConTypes.get(i);
                isFinalCharacter = true;
            }
            
            
            if (isFinalCharacter == true) {
                break;
            }
            //printSchwaSequence();
            if ( ( "U".equals(prevCon) && prevType.equals("CON")) &&  nextType.equals("HLT") ) {
           	 listConTypes.set(i, "H");
            } 
            //else if ( ("U".equals(prevCon) && isFullVowel(nextUchar)) ) {
           	// listConTypes.set(i, "F");
            //}
        }
	}

    private void RuleSetFour() {
      	 String prevType, nextType;
           String prevPhone, nextPhone;
           String prevUchar, nextUchar;
           String prevCon, nextCon;
           boolean isFinalCharacter = false;
           
           for(int i=0; i<listPhoneTypes.size(); i++){
          	 prevType  = listPhoneTypes.get(i);
               prevPhone = this.listPhoneSym.get(i);
               prevUchar = this.utf8CharList.get(i);
               prevCon = this.listConTypes.get(i);
               
               if ( (i+1) < listPhoneTypes.size() ) {
                   nextType = listPhoneTypes.get(i+1);
                   nextPhone = this.listPhoneSym.get(i+1);
                   nextUchar = this.utf8CharList.get(i+1);
                   nextCon = this.listConTypes.get(i+1);
               } else {
                   nextType = listPhoneTypes.get(i);
                   nextPhone = this.listPhoneSym.get(i);
                   nextUchar = this.utf8CharList.get(i);
                   nextCon = this.listConTypes.get(i);
                   isFinalCharacter = true;
               }
               
               
               if (isFinalCharacter == true) {
                   break;
               }
               //printSchwaSequence();
               if ( ("U".equals(prevCon) && isFullVowel(nextUchar)) ) {
              	 listConTypes.set(i, "F");
               }
           }
   	}
    
    private void RuleSetFive() {
     	 String prevType, nextType;
          String prevPhone, nextPhone;
          String prevUchar, nextUchar;
          String prevCon, nextCon;
          boolean isFinalCharacter = false;
          boolean encounterF = false;
          
          for(int i=0; i<listPhoneTypes.size(); i++){
         	 prevType  = listPhoneTypes.get(i);
              prevPhone = this.listPhoneSym.get(i);
              prevUchar = this.utf8CharList.get(i);
              prevCon = this.listConTypes.get(i);
              
              if ( (i+1) < listPhoneTypes.size() ) {
                  nextType = listPhoneTypes.get(i+1);
                  nextPhone = this.listPhoneSym.get(i+1);
                  nextUchar = this.utf8CharList.get(i+1);
                  nextCon = this.listConTypes.get(i+1);
              } else {
                  nextType = listPhoneTypes.get(i);
                  nextPhone = this.listPhoneSym.get(i);
                  nextUchar = this.utf8CharList.get(i);
                  nextCon = this.listConTypes.get(i);
                  isFinalCharacter = true;
              }
              
              
              if (isFinalCharacter == true) {
                  break;
              }
              
              if ("U".equals(prevCon) && !encounterF) {
            	  listConTypes.set(i, "F");
            	  break;
              } else if ("F".equals(prevCon)) {
            	  break;
              }
          }
  	}
    
    private void RuleSetSix() {
    	 String prevType, nextType;
         String prevPhone, nextPhone;
         String prevUchar, nextUchar;
         String prevCon, nextCon;
         boolean isFinalCharacter = false;
         boolean encounterF = false;
         int i = listPhoneTypes.size() - 1;
         prevType  = listPhoneTypes.get(i);
         prevPhone = this.listPhoneSym.get(i);
         prevUchar = this.utf8CharList.get(i);
         prevCon = this.listConTypes.get(i);
         
         if ( "U".equals(prevCon) && "CON".equals(prevType) ) {
        	 listConTypes.set(i, "H");
         }
        
 	}
    
    
    private void RuleSetSeven() {
    	 String prevType, nextType;
         String prevPhone, nextPhone;
         String prevUchar, nextUchar;
         String prevCon, nextCon;
         boolean isFinalCharacter = false;
         boolean encounterF = false;
         
         for(int i=0; i<listPhoneTypes.size(); i++){
        	 prevType  = listPhoneTypes.get(i);
             prevPhone = this.listPhoneSym.get(i);
             prevUchar = this.utf8CharList.get(i);
             prevCon = this.listConTypes.get(i);
             
             if ( (i+1) < listPhoneTypes.size() ) {
                 nextType = listPhoneTypes.get(i+1);
                 nextPhone = this.listPhoneSym.get(i+1);
                 nextUchar = this.utf8CharList.get(i+1);
                 nextCon = this.listConTypes.get(i+1);
             } else {
                 nextType = listPhoneTypes.get(i);
                 nextPhone = this.listPhoneSym.get(i);
                 nextUchar = this.utf8CharList.get(i);
                 nextCon = this.listConTypes.get(i);
                 isFinalCharacter = true;
             }
             
             
             if (isFinalCharacter == true) {
                 break;
             }
             
             if ("U".equals(prevCon) && "CON".equals(prevType)) {
            	 if ("CON".equals(nextType) && "H".equals(nextCon)) {
            		 listConTypes.set(i, "F");
            	 } else if ("SYM".equals(nextType) && "#".equals(nextCon)) {
            		 listConTypes.set(i, "F");
            	 }
             } 
             
         }
 	}
    
    private void RuleSetEight() {
   	 	String prevType, nextType, currentType;
        String prevPhone, nextPhone, currentPhone;
        String prevUchar, nextUchar, currentUchar;
        String prevCon, nextCon, currentCon;
        boolean isFinalCharacter = false;
        boolean encounterF = false;
        
        for(int i=1; i<listPhoneTypes.size()-1; i++){
        	prevType  = listPhoneTypes.get(i-1);
        	prevPhone = this.listPhoneSym.get(i-1);
        	prevUchar = this.utf8CharList.get(i-1);
        	prevCon = this.listConTypes.get(i-1);
        	
        	currentType  = listPhoneTypes.get(i);
        	currentPhone = this.listPhoneSym.get(i);
        	currentUchar = this.utf8CharList.get(i);
        	currentCon = this.listConTypes.get(i);

        	nextType = listPhoneTypes.get(i+1);
        	nextPhone = this.listPhoneSym.get(i+1);
        	nextUchar = this.utf8CharList.get(i+1);
        	nextCon = this.listConTypes.get(i+1);


            if (isFinalCharacter == true) {
                break;
            }
            
            if ( "U".equals(currentCon) ) {
            	if ("F".equals(prevCon) && "F".equals(nextCon)) {
            	 	listConTypes.set(i, "H");
            	} else {
            		listConTypes.set(i, "F");
            	}
            } 
         }
	}
    
    
	/**
     * print array list 
     * @param aList
     */
    private void printArrayList(ArrayList<String> aList){
        Iterator<String> listrun = aList.iterator();
        System.out.println();
        while(listrun.hasNext())
        {
            //System.out.print(listrun.next()+" ");
            System.out.print(" " + listrun.next());
        }
        System.out.println();
    }
    

    /**
     * verification for full vowel range based on unicode chart
     * @param uniCodeHex
     * @return
     */
    private boolean isFullVowel(String uniCodeHex) {
        int unicode    = hexString2Int(uniCodeHex);
        int minFVChart = hexString2Int("0904"); 
        int maxFVChart = hexString2Int("0914");
        
        if ( unicode >= minFVChart && unicode <= maxFVChart ) {
            return true;
        }
        return false;
    }
    
    /**
     * Hex-decimal representation for a given string
     * @param ch
     * @return
     */
    private String int2HexString(int ch) 
    {
        String hex = Integer.toHexString(ch).toUpperCase();
        switch (hex.length()) {
            case 3  : return "0" + hex;
            case 2  : return "00" + hex;
            case 1  : return "000" + hex;
            default : return hex;
        }
    }
    
    /**
     * converting hexcode to integer value
     * @param hexCode
     * @return
     */
    private int hexString2Int (String hexCode) {
        return Integer.parseInt(hexCode, 16);  
    }

    private void printSchwaSequence() {
    	if ( (this.listPhoneSym.size() != this.listConTypes.size()) 
    			|| (this.listPhoneSym.size() != this.utf8CharList.size()) 
    			|| (this.listPhoneSym.size() != this.listPhoneTypes.size()) ) {
    		System.err.println(utf8CharList.size()+" "+listPhoneSym.size()
    				+" "+listPhoneTypes.size()+ " "+listConTypes.size());
    		throw new RuntimeException("Array list sizes doesnot match !!!");
    	}
    	System.out.println("***************");
    	for ( int i=0; i<this.utf8CharList.size(); i++ ) {
    		System.out.println(utf8CharList.get(i)+" "+listPhoneSym.get(i)
    				+" "+listPhoneTypes.get(i)+ " "+listConTypes.get(i));
    	}
    }
    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        
        //HindiLTS utf8r = new  HindiLTS(new FileInputStream("~/openmary/lib/modules/te/lexicon/UTF8phone.te.list"));
        HindiLTS utf8r = new  HindiLTS(new FileInputStream("/Users/sathish/Work/BitBucket/marytts/marytts-lang-hi/src/main/resources/marytts/language/hi/lexicon/UTF8toIT3.hi.list"));
        //utf8r.makeProperIt3("/home/sathish/Desktop/telugu-utf8-txt.done.data");
        //String nameString = "\u0C05\u0C38\u0C1F\u0C08\u0C05\u0C37";
        //PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("telugu-utf.txt"), "UTF8"));
        //pw.print(nameString);
        //pw.flush();
        //pw.close();
        
       System.out.println("Result : "+utf8r.phonemise("आपका"));
      System.out.println("Result : "+utf8r.phonemise("बचपन"));
      System.out.println("Result : "+utf8r.phonemise("प्रियतम"));
       System.out.println("Result : "+utf8r.phonemise("आमंत्रण"));
      System.out.println("Result : "+utf8r.phonemise("कतई"));
      System.out.println("Result : "+utf8r.phonemise("हूँ"));
      utf8r.makeProperIt3("/Users/sathish/Work/BitBucket/delme/text/hin_0001.txt");
        
    }

}
