package de.rwth.idsg.adapter.manage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AlternativeWSDLParser {
    /**
    * @param args the command line arguments
    */
    public String[] listOperations(String filename) throws FileNotFoundException, SAXException,IOException, ParserConfigurationException 
    {
       Document d = (Document) DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(filename));
       NodeList elements = d.getElementsByTagName("wsdl:operation");
       ArrayList<String> operations = new ArrayList<String>();
       for (int i = 0; i < elements.getLength(); i++) {
           operations.add(elements.item(i).getAttributes().getNamedItem("name").getNodeValue());
       }
       return operations.toArray(new String[operations.size()]);
    }
    public String[] listOperationsUnique(String filename) throws   FileNotFoundException,SAXException, IOException, ParserConfigurationException 
    {
       String[] nonUnique = listOperations(filename);
       HashSet<String> unique = new HashSet<String>(Arrays.asList(nonUnique));
       return unique.toArray(new String[unique.size()]);
    }
    public String[] listInputs(String filename) throws FileNotFoundException, SAXException,IOException, ParserConfigurationException, NullPointerException 
    {
       Document d1 = (Document) DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(filename));
       NodeList elements1 = d1.getElementsByTagName("wsdl:input");
       NodeList elements ;
       NodeList ele = d1.getElementsByTagName("wsdl:message");        
       //ArrayList<String> tinput = new ArrayList<>();       
       ArrayList<String> tput = new ArrayList<String>();
       ArrayList<String> input = new ArrayList<String>();
       for(int k=0; k < elements1.getLength(); k++)
       {
           if(elements1.item(k).getAttributes().getNamedItem("message") != null)
           {
               input.add(elements1.item(k).getAttributes().getNamedItem("message").getNodeValue());
           }
       }
       String[] s,s1,s2;
       s1 = input.toArray(new String[input.size()]);
       s= this.listMessages(filename);
       for(int i=0;i<s1.length;i++)
       {
           for(int j=0;j<s.length;j++)
           {
               if((s1[i].substring(4)).equals(s[j]))
               {
                 for (int k = 0; k < ele.getLength(); k++) 
                 {
                   if(ele.item(k).getAttributes().getNamedItem("name").getNodeValue().equals(s[j]))
                   {
                       elements = ele.item(k).getChildNodes();
                       for(int l=0; l < elements.getLength(); l++)
                       {
                           if(elements.item(l).getAttributes() != null)
                           {
                               tput.add(elements.item(l).getAttributes().getNamedItem("name").getNodeValue());
                           }
                       }            
                   }
                 }   
               }
           }
       }
       s2 = tput.toArray(new String[tput.size()]);
       return s2;
       //return input.toArray(new String[input.size()]);
    }

    public String[] listMessages(String filename) throws FileNotFoundException, SAXException,IOException, ParserConfigurationException 
    {
       Document d = (Document) DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(filename));
       NodeList elements = d.getElementsByTagName("wsdl:message");
       ArrayList<String> messages = new ArrayList<String>();
       for (int i = 0; i < elements.getLength(); i++) 
       {
           if(elements.item(i).getAttributes().getNamedItem("name") != null)
           {
               messages.add(elements.item(i).getAttributes().getNamedItem("name").getNodeValue());
           }
       }
       return messages.toArray(new String[messages.size()]);
    }
    public String[] listMinputs(String filename) throws FileNotFoundException, SAXException,IOException, ParserConfigurationException, NullPointerException 
    {
       Document d1 = (Document) DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(filename));
       NodeList melements1 = d1.getElementsByTagName("wsdl:part");
       ArrayList<String> minput = new ArrayList<String>();
       int l,i;
       l = melements1.getLength();
       for(int k=0; k < l; k++)
       {
           if(melements1.item(k).getAttributes().getNamedItem("element") != null)
           {
               minput.add(melements1.item(k).getAttributes().getNamedItem("element").getNodeValue());
           }
       }
        return minput.toArray(new String[minput.size()]);
     }

   /**
    *
    * @param args
    * @throws FileNotFoundException
    * @throws SAXException
    * @throws IOException
    * @throws ParserConfigurationException
    */
   public static void main(String[] args) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, NullPointerException 
   {
       // TODO code application logic here
       AlternativeWSDLParser p;
       p = new AlternativeWSDLParser();
       System.out.println("Hi");
       String uri;
       uri = "C:\\Users\\New\\Desktop\\xquotes.asmx.xml";
       String[] s, s1;
       s = p.listOperationsUnique(uri);
       s1 = p.listInputs(uri);
       int len = s.length;
       int len1 = s1.length;
       System.out.println("Operations:");
       for(int i=0; i<len; i++)
       {
          System.out.println(s[i]); 
       }
       System.out.println("Input Params:");
       for(int i=0; i<len1; i++)
       {
          System.out.println(s1[i]); 
       }
   }
}
