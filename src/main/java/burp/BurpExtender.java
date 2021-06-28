package burp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class BurpExtender
  implements IBurpExtender, IContextMenuFactory
{
private IBurpExtenderCallbacks callbacks;
  private IExtensionHelpers helpers;
  
  public static void main(String[] args) {}
  
  public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
  {
    this.callbacks = callbacks;
    this.helpers = callbacks.getHelpers();
    callbacks.setExtensionName("Dirlist Exporter");
    callbacks.registerContextMenuFactory(this);
  }
  
  public void writeStringToFile(String Output, File file)
  {
    BufferedWriter out = null;
    try
    {
      out = new BufferedWriter(new FileWriter(file));
      out.write(Output);
      JOptionPane.showMessageDialog(null, "File saved successfully."); return;
    }
    catch (IOException e1)
    {
      JOptionPane.showMessageDialog(null, "Error saving file: " + e1.getMessage());
    }
    finally
    {
      try
      {
        out.close();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }
  
  public String createOutputForWordlist(IHttpRequestResponse[] tmp)
  {
    List<String> stringArray = new ArrayList<String>();
    for (int i = 0; i < tmp.length; i++)
    {
      String path = "/";
      String tmpStr = new String(tmp[i].getRequest());
      
      int firstspace = tmpStr.indexOf(" ");
      int secondspace = tmpStr.indexOf(" ", firstspace + 1);
      int questionmark = tmpStr.indexOf("?", firstspace + 1);
      int lastslash = tmpStr.lastIndexOf("/", secondspace - 1);
      if ((questionmark < secondspace) && (questionmark > 0)) {
    	  secondspace = questionmark;
      }
      
      if (firstspace + 1 == lastslash) {
    	  continue;
    	  // No directory found
      }
      
      path = tmpStr.substring(firstspace + 1, lastslash).replace("\"", "%22");

      for (int j = 0; j < path.split("/").length; j++)
      {
    	  stringArray.add(path.split("/")[j]);
      }
    }
    
    String[] simpleArray = new String[ stringArray.size() ];
    stringArray.toArray( simpleArray );
    
    // We only want a single instance of each word
    Set<String> simpleArray2 = new HashSet<String>(Arrays.asList(simpleArray));
    String[] uq = simpleArray2.toArray(new String[simpleArray2.size()]);
    
    // Sort directory names
    Arrays.sort(uq, new Comparator<String>() {
        @Override
        public int compare(String first, String second) {
            return first.toLowerCase().compareTo(second.toLowerCase());
        }
    });
    
    String Output = "";
    for(int i = 0; i < uq.length; i++)
    {
    	if(uq[i] == "" || uq[i].contains("?")) { continue; }
    	if(i+1 < uq.length) {
    		Output = Output + uq[i] + "\n";
    	} else {
    		Output = Output + uq[i];
    	}
    }
    
    return Output;
  }
  
  public ArrayList<JMenuItem> createMenuItems(final IContextMenuInvocation invocation)
  {
    ArrayList<JMenuItem> menu = new ArrayList();
    
    byte ctx = invocation.getInvocationContext();
    
    JMenu main = new JMenu("Dirlist Exporter");
    JMenuItem item = new JMenuItem("Export Site Map", null);
    
    FileFilter filter = new FileNameExtensionFilter("TXT File", new String[] { "txt" });
    
    final JFileChooser fileChooser = new JFileChooser()
    {
      public void approveSelection()
      {
        File f = getSelectedFile();
        if ((f.exists()) && (getDialogType() == 1))
        {
          int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file", 1);
          switch (result)
          {
          case 0: 
            super.approveSelection();
            return;
          case 1: 
            return;
          case -1: 
            return;
          case 2: 
            cancelSelection();
            return;
          }
        }
        super.approveSelection();
      }
    };
    fileChooser.setFileSelectionMode(2);
    fileChooser.setMultiSelectionEnabled(false);
    fileChooser.setFileFilter(filter);
    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
    fileChooser.setSelectedFile(new File("custom_Dirlist_from_Burp.txt"));
    
    item.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        IHttpRequestResponse tmp2 = invocation.getSelectedMessages()[0];
        IHttpRequestResponse[] tmp = BurpExtender.this.callbacks.getSiteMap(tmp2.getHttpService().getProtocol() + "://" + tmp2.getHttpService().getHost());
        if (fileChooser.showSaveDialog(null) == 0)
        {
          File outputFile = fileChooser.getSelectedFile();
          BurpExtender.this.writeStringToFile(BurpExtender.this.createOutputForWordlist(tmp), outputFile);
        }
      }
    });
    main.add(item);
    menu.add(main);

    return menu;
  }
}
