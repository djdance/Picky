/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.orgcom.picky;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rasendori
 */
public class Converter {
    public String string ="";

    public Converter() {
    }
    public String convert(String string1) {
        string = string1.replace("а", "a");
        string = string.replace("б", "b");
        string = string.replace("в", "v");
        string = string.replace("г", "g");
        string = string.replace("д", "d");
        string = string.replace("е", "e");
        string = string.replace("ё", "yo");
        string = string.replace("ж", "zh");
        string = string.replace("з", "z");
        string = string.replace("и", "i");
        string = string.replace("й", "i");
        string = string.replace("к", "k");
        string = string.replace("л", "l");
        string = string.replace("м", "m");
        string = string.replace("н", "n");
        string = string.replace("о", "o");
        string = string.replace("п", "p");
        string = string.replace("р", "r");
        string = string.replace("с", "s");
        string = string.replace("т", "t");
        string = string.replace("у", "u");
        string = string.replace("ф", "f");
        string = string.replace("х", "h");
        string = string.replace("ц", "c");
        string = string.replace("ч", "ch");
        string = string.replace("ш", "sch");
        string = string.replace("щ", "sch");
        string = string.replace("ь", "'");
        string = string.replace("ы", "y");
        string = string.replace("ъ", "'");
        string = string.replace("э", "e");
        string = string.replace("ю", "yu");
        string = string.replace("я", "ya");
        string = string.replace("А", "A");
        string = string.replace("Б", "B");
        string = string.replace("В", "V");
        string = string.replace("Г", "G");
        string = string.replace("Д", "D");
        string = string.replace("Е", "E");
        string = string.replace("Ё", "YO");
        string = string.replace("Ж", "ZH");
        string = string.replace("З", "Z");
        string = string.replace("И", "I");
        string = string.replace("Й", "I");
        string = string.replace("К", "K");
        string = string.replace("Л", "L");
        string = string.replace("М", "M");
        string = string.replace("Н", "N");
        string = string.replace("О", "O");
        string = string.replace("П", "P");
        string = string.replace("Р", "R");
        string = string.replace("С", "S");
        string = string.replace("Т", "T");
        string = string.replace("У", "U");
        string = string.replace("Ф", "F");
        string = string.replace("Х", "H");
        string = string.replace("Ц", "C");
        string = string.replace("Ч", "CH");
        string = string.replace("Ш", "SCH");
        string = string.replace("щ", "sch");
        string = string.replace("Ь", "'");
        string = string.replace("Ы", "y");
        string = string.replace("Ъ", "'");
        string = string.replace("Э", "e");
        string = string.replace("Ю", "yu");
        string = string.replace("Я", "ya");
        return string;
    }
}
