package com.jnotepad;

import com.jnotepad.menus.AboutFrame;
import com.jnotepad.menus.CountFrame;
import com.jnotepad.menus.FindFrame;
import com.jnotepad.menus.PrintPreviewFrame;
import com.jnotepad.menus.ReplaceFrame;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.print.PrintServiceLookup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import say.swing.JFontChooser;

/**
 *
 * @author Anthony Canicatti <a.canicatti@gmail.com>
 */
public class JavaNotepad extends javax.swing.JFrame {
    
    static Highlighter h; // for Find tool
    static DefaultHighlighter.DefaultHighlightPainter hPainter; // for Find tool
    static ConsoleFrame console; // output console
    boolean consoleVisible = false; // flag for visibility of console
    JPopupMenu popupMenu; // textArea popup menu on right click
    JCheckBoxMenuItem wordWrapPopupItem; // popup item for word wrap - wordWrapItem must have access to this
    static int currItemFound = 0; // for Find tool 
    PopupMenu trayPopup;
    SystemTray sysTray = null;
    TrayIcon trayIcon;
    boolean wasIconified = false;
    JavaNotepad myself = this;
    PrefUtility prefs;
    boolean editorMode = false;
    
    int lineNum = 1;
    
    
    /**
     * Creates new form JavaNotepad
     */
    public JavaNotepad() {
        initComponents();
        this.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                saveSizeAndLoc();
                console.dispose();
                dispose();
            }
        });
        setIcons();
        prefs = PrefUtility.getInstance();
        getSizeAndLoc();
        
        hPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
        
        cutItem.setEnabled(false);
        copyItem.setEnabled(false);
        clearTextItem.setEnabled(false);
        clearHighlightingItem.setEnabled(false);
        
        if(currentFileOpen == null)
            saveItem.setEnabled(false);
        
        console = new ConsoleFrame(this);
        console.setVisible(false);
        console.appendToConsole("JavaNotepad initialized...");
        console.appendToConsole("Console initialized...");
        
        popupMenu = new JPopupMenu();
        setUpPopup();
        textArea.setComponentPopupMenu(popupMenu);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        h = textArea.getHighlighter();
        
        addMouseListenerToTextArea();
        addKeyListenerToTextArea();
        
        lineNumsField.setVisible(false);
        lineNumsScroll.setVisible(false);
    }
    
    /**
     * Save size and location to preferences
     */
    private void saveSizeAndLoc(){
        int width = myself.getWidth();
        int height = myself.getHeight();
        String size = ""+width+","+height;
        prefs.setSize(size);
        
        int x = myself.getX();
        int y = myself.getY();
        String loc = ""+x+","+y;
        prefs.setLoc(loc);
    }
    
    /**
     * Retrieve size and location from preferences
     */
    private void getSizeAndLoc(){
        String size = prefs.getSize();
        String[] sizeParts = size.split(",");
        int width = Integer.parseInt(sizeParts[0]);
        int height = Integer.parseInt(sizeParts[1]);
        this.setSize(width, height);
        
        String loc = prefs.getLoc();
        if(loc.equals("default")){
            this.setLocationRelativeTo(null);
            return;
        }
        String[] locParts = loc.split(",");
        int x = Integer.parseInt(locParts[0]);
        int y = Integer.parseInt(locParts[1]);
        this.setLocation(x, y);
    }
    
    /**
     * Set icons for frame
     */
    private void setIcons(){
        ArrayList<Image> iconList = new ArrayList<>();
        iconList.add(new ImageIcon("res/icons/jn16.png").getImage());
        iconList.add(new ImageIcon("res/icons/jn32.png").getImage());
        iconList.add(new ImageIcon("res/icons/jn64.png").getImage());
        iconList.add(new ImageIcon("res/icons/jn128.png").getImage());
        iconList.add(new ImageIcon("res/icons/jn256.png").getImage());
        this.setIconImages(iconList);
    }
    
    /**
     * Configure right click Pop-up menu in JTextArea
     */
    private void setUpPopup(){
        JMenuItem fontPopupItem = new JMenuItem("Font...");
        fontPopupItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                fontItemActionPerformed(e);
            }
        });
        JMenuItem textColorPopupItem = new JMenuItem("Text Color...");
        textColorPopupItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                textColorItemActionPerformed(e);
            }
        });
        JMenuItem backColorPopupItem = new JMenuItem("Background Color...");
        backColorPopupItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                backColorItemActionPerformed(e);
            }
        });
        wordWrapPopupItem = new JCheckBoxMenuItem("Word Wrap");
        wordWrapPopupItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                boolean selected = wordWrapPopupItem.getState();
                textArea.setLineWrap(selected);
                textArea.setWrapStyleWord(selected);
                wordWrapItem.setState(selected);
            }
        });
        wordWrapPopupItem.setState(true);
        
        JMenuItem findPopupItem = new JMenuItem("Find");
        findPopupItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                findItemActionPerformed(e);
            }
        });
        JMenuItem replacePopupItem = new JMenuItem("Replace");
        replacePopupItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                replaceItemActionPerformed(e);
            }
        });
        JMenuItem wordCountPopupItem = new JMenuItem("Word Count");
        wordCountPopupItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                wordCountItemActionPerformed(e);
            }
        });
        
        popupMenu.add(fontPopupItem);
        popupMenu.add(textColorPopupItem);
        popupMenu.add(backColorPopupItem);
        popupMenu.add(wordWrapPopupItem);
        popupMenu.addSeparator();
        popupMenu.add(findPopupItem);
        popupMenu.add(replacePopupItem);
        popupMenu.add(wordCountPopupItem);
    }
    
    /**
     * Configure tray icon
     */
    private void setUpTray(){
        BufferedImage iconImage = null;
        int iconWidth = 0;
        try {
            iconImage = ImageIO.read(new File("res/icons/jn64.png"));
            iconWidth = (int) new TrayIcon(iconImage).getSize().getWidth();
        } catch (IOException ex) {
            Logger.getLogger(JavaNotepad.class.getName()).log(Level.SEVERE, null, ex);
        }
        trayIcon = new TrayIcon(iconImage.getScaledInstance(iconWidth, -1, Image.SCALE_SMOOTH));
        sysTray = SystemTray.getSystemTray();
        trayPopup = new PopupMenu();
        
        MenuItem open = new MenuItem("Open JNotepad");
        open.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                myself.setVisible(true);
                myself.setState(Frame.NORMAL);
            }
        });
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                System.exit(0);
            }
        });
        trayPopup.add(open);
        trayPopup.add(exit);
        trayIcon.setPopupMenu(trayPopup);
        try {
            sysTray.add(trayIcon);
        } catch (AWTException ex) {
            Logger.getLogger(JavaNotepad.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Destroy tray icon
     */
    private void destroyTray(){
        if(sysTray != null){
            sysTray.remove(trayIcon);
            sysTray = null;
        }
    }
    
    private void addMouseListenerToTextArea(){
        
        textArea.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                
                cutItem.setEnabled(false);
                copyItem.setEnabled(false);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                if(textArea.getSelectedText() != null){
                    cutItem.setEnabled(true);
                    copyItem.setEnabled(true);
                    console.appendToConsole("Mouse released. Selected text: "+textArea.getSelectedText());
                }
            }
        });
        
        textArea.getDocument().addDocumentListener(new DocumentListener(){
            @Override
            public void insertUpdate(DocumentEvent e) {
                if(textArea.getText() != null || (!textArea.getText().isEmpty())){
                    clearTextItem.setEnabled(true);
                }
                updated();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if(textArea.getText() != null || (!textArea.getText().isEmpty())){
                    clearTextItem.setEnabled(true);
                }
                updated();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {}
            public void updated(){}
        });
    }

    private String getLastWord(){
        String word = "";
        ArrayList<Character> wordArr = new ArrayList<>();
        int start = textArea.getCaretPosition();
        while(true){
            if(start > 0){
                start--;
                char c = textArea.getText().charAt(start);
                if(c != ' ' && start != 0)
                    wordArr.add(c);
                else if(start == 0)
                    return textArea.getText();
                else
                    break;
            }
        }
        Collections.reverse(wordArr);
        for(char c : wordArr)
            word+=c;
        return word;
    }
    
    private void addKeyListenerToTextArea(){
        KeyListener keyListener = new KeyAdapter(){
            @Override
            public void keyReleased(KeyEvent e){
                if(!editorMode)
                    return;
                if(e.getKeyCode()==KeyEvent.VK_BACK_SPACE){
                    setUpLineNums();
                    return;
                }
                else if(e.getKeyCode()==KeyEvent.VK_ENTER){
                    handleEnter();
                    setUpLineNums();
                }
                
                char typed = e.getKeyChar();
                if(typed == '{'){
                    handleOpenBrace();
                    setUpLineNums();
                }
                else if(typed == '(')
                    handleOpenParens();
                else if(typed == '*')
                    handleCommentBlock();
                else if(typed == '"')
                    handleOpenQuote();
                else if(typed == ';')
                    handleSemiColon();
                
            }
        };
        textArea.addKeyListener(keyListener);
    }
    
    private void handleSemiColon(){
        try {
            int line = textArea.getLineOfOffset(textArea.getCaretPosition());
            int start = textArea.getLineStartOffset(line);
            int end = textArea.getLineEndOffset(line);
            String text = textArea.getText(start, end-start);
            text = text.replace("\t", "").replace("\n", "");
            if(textArea.getCaretPosition() != (end-start)){
                if(!text.endsWith(";")){
                    if(text.contains("(") && text.contains(")")){
                        textArea.getDocument().remove(textArea.getCaretPosition()-1, 1);
                        textArea.insert(";", end-2);
                        textArea.setCaretPosition(end-1);
                    }
                }
            }
            System.out.println("Text from semicolon: "+text);
        } catch(BadLocationException ble){
            System.err.println("BadLocation: "+ble.getLocalizedMessage());
        }
    }
    
    private void handleEnter(){
        try {
            tabOver(textArea.getCaretPosition());
            int line = textArea.getLineOfOffset(textArea.getCaretPosition())+1;
            int start = textArea.getLineStartOffset(line);
            int end = textArea.getLineEndOffset(line);
            String lineText = textArea.getText(start, end - start);
            lineText = lineText.replace("\t", "");
            System.out.println("Line text from Enter: "+lineText);
            if(lineText.startsWith("/*")||lineText.startsWith("*"))
                textArea.insert("* ", textArea.getCaretPosition());
        } catch (BadLocationException ex) {
            System.err.println("BadLocation: "+ex.getLocalizedMessage());
        }
    }

    private void handleOpenBrace(){
        textArea.insert("\n", textArea.getCaretPosition());
        tabOver(textArea.getCaretPosition());
        int pos = textArea.getCaretPosition();
        textArea.insert("\n", textArea.getCaretPosition());
        
        for(int i=0; i<getNumTabsBefore(textArea.getCaretPosition())-1; i++)
            textArea.insert("\t", textArea.getCaretPosition());
        textArea.insert("}", textArea.getCaretPosition());
        textArea.setCaretPosition(pos);
    }
    
    private void handleOpenParens(){
        int pos = textArea.getCaretPosition();
        textArea.insert(")", pos);
        textArea.setCaretPosition(pos);
    }
    
    private void handleCommentBlock(){
        String word = getLastWord();
        System.out.println("Last word: "+word);
        if(word.contains("/*")){
            System.out.println("I'm here");
            textArea.insert("\n", textArea.getCaretPosition());
            tabOver(textArea.getCaretPosition());
            int pos1 = textArea.getCaretPosition();
            textArea.insert("* \n", pos1);
            tabOver(textArea.getCaretPosition());
            textArea.insert("*/", textArea.getCaretPosition());
            textArea.setCaretPosition(pos1+2);
            setUpLineNums();
        }
    }
    
    private void handleOpenQuote(){
        int pos = textArea.getCaretPosition();
        textArea.insert("\"", pos);
        textArea.setCaretPosition(pos);
    }
    
    private void tabOver(int pos){
        int numTimes = getNumTabsBefore(pos);
        System.out.println("times to tab over: "+numTimes);
        for(int i=0; i<numTimes; i++)
            textArea.insert("\t", textArea.getCaretPosition());
    }
    
    private int getNumTabsBefore(int index){
        String allText = textArea.getText().substring(0, index);
        System.out.println("Evaluating text: "+allText);
        int lastIndex = 0;
        int numTabs=0;
        while(lastIndex != -1){
            lastIndex = allText.indexOf("{", lastIndex);
            if(lastIndex != -1){
                numTabs++;
                lastIndex+=1;
            }
        }
        for(int i=0; i<allText.length(); i++){
            char c = allText.charAt(i);
            if(c == '}')
                numTabs--;
        }
        return numTabs;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        jScrollPane1 = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();
        statusPanel = new javax.swing.JPanel();
        fileOpenLabel = new javax.swing.JLabel();
        fileWriteLabel = new javax.swing.JLabel();
        lineNumsScroll = new javax.swing.JScrollPane();
        lineNumsField = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openFileItem = new javax.swing.JMenuItem();
        saveItem = new javax.swing.JMenuItem();
        saveAsItem = new javax.swing.JMenuItem();
        printPreviewItem = new javax.swing.JMenuItem();
        printItem = new javax.swing.JMenuItem();
        exitItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutItem = new javax.swing.JMenuItem();
        copyItem = new javax.swing.JMenuItem();
        pasteItem = new javax.swing.JMenuItem();
        clearTextItem = new javax.swing.JMenuItem();
        clearHighlightingItem = new javax.swing.JMenuItem();
        formatMenu = new javax.swing.JMenu();
        fontItem = new javax.swing.JMenuItem();
        textColorItem = new javax.swing.JMenuItem();
        backColorItem = new javax.swing.JMenuItem();
        highlighterColorItem = new javax.swing.JMenuItem();
        wordWrapItem = new javax.swing.JCheckBoxMenuItem();
        toolsMenu = new javax.swing.JMenu();
        findItem = new javax.swing.JMenuItem();
        replaceItem = new javax.swing.JMenuItem();
        wordCountItem = new javax.swing.JMenuItem();
        developerMenu = new javax.swing.JMenu();
        consoleItem = new javax.swing.JMenuItem();
        editorModeItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutItem = new javax.swing.JMenuItem();

        fileChooser.setFileFilter(new CustomFilter());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Java Notepad");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                formComponentMoved(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowIconified(java.awt.event.WindowEvent evt) {
                formWindowIconified(evt);
            }
            public void windowDeiconified(java.awt.event.WindowEvent evt) {
                formWindowDeiconified(evt);
            }
        });

        textArea.setColumns(20);
        textArea.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        textArea.setRows(5);
        jScrollPane1.setViewportView(textArea);

        fileOpenLabel.setText("No file open.");

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileOpenLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(fileWriteLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileOpenLabel)
                    .addComponent(fileWriteLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lineNumsScroll.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        lineNumsScroll.setHorizontalScrollBar(null);

        lineNumsField.setEditable(false);
        lineNumsField.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        lineNumsField.setColumns(1);
        lineNumsField.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        lineNumsField.setRows(5);
        lineNumsField.setFocusable(false);
        lineNumsScroll.setViewportView(lineNumsField);

        fileMenu.setText("File");

        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenuItem.setText("New");
        newMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newMenuItem);

        openFileItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openFileItem.setText("Open...");
        openFileItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileItemActionPerformed(evt);
            }
        });
        fileMenu.add(openFileItem);

        saveItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveItem.setText("Save");
        saveItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveItem);

        saveAsItem.setText("Save As...");
        saveAsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsItem);

        printPreviewItem.setText("Print Preview");
        printPreviewItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printPreviewItemActionPerformed(evt);
            }
        });
        fileMenu.add(printPreviewItem);

        printItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        printItem.setText("Print...");
        printItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printItemActionPerformed(evt);
            }
        });
        fileMenu.add(printItem);

        exitItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        exitItem.setText("Exit");
        exitItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");

        cutItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        cutItem.setText("Cut");
        cutItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cutItemActionPerformed(evt);
            }
        });
        editMenu.add(cutItem);

        copyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyItem.setText("Copy");
        copyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyItemActionPerformed(evt);
            }
        });
        editMenu.add(copyItem);

        pasteItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pasteItem.setText("Paste");
        pasteItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteItemActionPerformed(evt);
            }
        });
        editMenu.add(pasteItem);

        clearTextItem.setText("Clear Text");
        clearTextItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearTextItemActionPerformed(evt);
            }
        });
        editMenu.add(clearTextItem);

        clearHighlightingItem.setText("Clear Highlighting");
        clearHighlightingItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearHighlightingItemActionPerformed(evt);
            }
        });
        editMenu.add(clearHighlightingItem);

        menuBar.add(editMenu);

        formatMenu.setText("Format");

        fontItem.setText("Font...");
        fontItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fontItemActionPerformed(evt);
            }
        });
        formatMenu.add(fontItem);

        textColorItem.setText("Text Color...");
        textColorItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textColorItemActionPerformed(evt);
            }
        });
        formatMenu.add(textColorItem);

        backColorItem.setText("Background Color...");
        backColorItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backColorItemActionPerformed(evt);
            }
        });
        formatMenu.add(backColorItem);

        highlighterColorItem.setText("Highlighter Color...");
        highlighterColorItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                highlighterColorItemActionPerformed(evt);
            }
        });
        formatMenu.add(highlighterColorItem);

        wordWrapItem.setSelected(true);
        wordWrapItem.setText("Word Wrap");
        wordWrapItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wordWrapItemActionPerformed(evt);
            }
        });
        formatMenu.add(wordWrapItem);

        menuBar.add(formatMenu);

        toolsMenu.setText("Tools");

        findItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        findItem.setText("Find...");
        findItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findItemActionPerformed(evt);
            }
        });
        toolsMenu.add(findItem);

        replaceItem.setText("Replace...");
        replaceItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceItemActionPerformed(evt);
            }
        });
        toolsMenu.add(replaceItem);

        wordCountItem.setText("Word Count");
        wordCountItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wordCountItemActionPerformed(evt);
            }
        });
        toolsMenu.add(wordCountItem);

        developerMenu.setText("Developer");

        consoleItem.setText("Show/Hide Console");
        consoleItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleItemActionPerformed(evt);
            }
        });
        developerMenu.add(consoleItem);

        editorModeItem.setText("Editor Mode");
        editorModeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editorModeItemActionPerformed(evt);
            }
        });
        developerMenu.add(editorModeItem);

        toolsMenu.add(developerMenu);

        menuBar.add(toolsMenu);

        helpMenu.setText("Help");

        aboutItem.setText("About");
        aboutItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lineNumsScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 830, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                        .addGap(1, 1, 1))
                    .addComponent(lineNumsScroll))
                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitItemActionPerformed
        // TODO add your handling code here:
        
        this.dispose();
        console.dispose();
    }//GEN-LAST:event_exitItemActionPerformed

    private void openFileItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileItemActionPerformed
        // TODO add your handling code here:
        
        int returnVal = fileChooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION){
            
            File file = fileChooser.getSelectedFile();
            console.appendToConsole("File opened: "+file.getAbsolutePath());
            console.appendToConsole("Current File Path: "+file.getAbsolutePath());
            currentFileOpen = file;
            try{
                textArea.read(new FileReader(file.getAbsolutePath()), null);
            }
            catch(IOException e){
                textArea.setText("Error reading file: "+file.getAbsolutePath());
                JOptionPane.showMessageDialog(textArea, "Error reading file: "+file.getAbsolutePath());
                console.appendToConsole("Error reading file: "+file.getAbsolutePath()+
                        "\nException information: "+e.getLocalizedMessage());
            }
            fileOpenLabel.setText("File open: "+file.getAbsolutePath()+".");
            saveItem.setEnabled(true);
        }
        else
            console.appendToConsole("File open cancelled by user.");
    }//GEN-LAST:event_openFileItemActionPerformed

    private void saveItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveItemActionPerformed
        // TODO add your handling code here:
        
        save();
    }//GEN-LAST:event_saveItemActionPerformed

    private void save(){
        
        if(currentFileOpen != null){

            FileWriter writer = null;
            try {
                String str = textArea.getText();
                writer = new FileWriter(currentFileOpen.getAbsolutePath());
                writer.write(str);
          
            } catch (IOException ex) {
                Logger.getLogger(JavaNotepad.class.getName()).log(Level.SEVERE, null, ex);
                console.appendToConsole("Error saving. Exception information: "+ex.getLocalizedMessage());
            } finally {
                try {
                    writer.close();
                    showWriteLabel(currentFileOpen);
                    console.appendToConsole("File: "+currentFileOpen.getAbsolutePath()+" saved to disk.");
                } catch (IOException ex) {
                    Logger.getLogger(JavaNotepad.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }
        else
            JOptionPane.showMessageDialog(jScrollPane1, "Open a file to save!", "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void saveAs(){
     
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter;
        if(editorMode)
            filter = new FileNameExtensionFilter("Code File (*.java, *.cpp, *.py, etc)", " ");
        else
            filter = new FileNameExtensionFilter("Text File (*.txt)", "txt");
        chooser.setFileFilter(filter);
        
        int returnVal = chooser.showSaveDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION){
            
            File file = chooser.getSelectedFile();
            String extension = editorMode == true ? "" : ".txt";
            String fileName = file.getAbsolutePath()+extension;
            File f = new File(fileName);
            currentFileOpen = f;
            saveItem.setEnabled(true);
            console.appendToConsole("File path: "+fileName);
            
            try{
                textArea.write(new FileWriter(f));
            } catch(FileNotFoundException e){
                console.appendToConsole("Error saving "+f.getAbsolutePath()+"\nException information: "+e.getLocalizedMessage());
            } catch (IOException ex) {
                Logger.getLogger(JavaNotepad.class.getName()).log(Level.SEVERE, null, ex);
            }
            showWriteLabel(f);
            fileOpenLabel.setText("File open: "+f.getAbsolutePath()+".");
        }
    }
    
    private void saveAsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsItemActionPerformed
        // TODO add your handling code here:
        
        saveAs();
    }//GEN-LAST:event_saveAsItemActionPerformed

    private void showWriteLabel(File f){
        
        fileWriteLabel.setText("Wrote to file: "+f.getName());
        console.appendToConsole("I'm here");
        Timer t = new Timer(5000, new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                fileWriteLabel.setText("");
            }
        });
        t.setRepeats(false);
        t.start();
    }
    
    private void clearTextItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearTextItemActionPerformed
        // TODO add your handling code here:
        
        textArea.setText("");
    }//GEN-LAST:event_clearTextItemActionPerformed

    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        // TODO add your handling code here:

        new JavaNotepad().setVisible(true);
        
    }//GEN-LAST:event_newMenuItemActionPerformed

    private void aboutItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutItemActionPerformed
        // TODO add your handling code here:
        AboutFrame about = new AboutFrame(this);
        about.setVisible(true);
        about.setResizable(false);
    }//GEN-LAST:event_aboutItemActionPerformed

    private void fontItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontItemActionPerformed
        // TODO add your handling code here:
        Font currentFont = textArea.getFont();
        JFontChooser fontChooser = new JFontChooser();
        fontChooser.setSelectedFont(currentFont);
        
        int frameX = this.getSize().width;
        int frameY = this.getSize().height;
        frameX *= 0.75;
        frameY *= 0.75;
        console.appendToConsole("FrameX: "+frameX+"; Frame Y: "+frameY);
        fontChooser.setSize(new Dimension(frameX, frameY));
        
        int returnVal = fontChooser.showDialog(this);
        if(returnVal == JFontChooser.OK_OPTION){
            Font selectedFont = fontChooser.getSelectedFont();
            textArea.setFont(selectedFont);
            lineNumsField.setFont(selectedFont);
        }
    }//GEN-LAST:event_fontItemActionPerformed

    /**
     * Replace tool
     * 
     * @param oldS old string to replace 
     * @param newS new string to replace old string
     * @return if the old string was found and replaced in the text
     */
    public boolean replaceItems(String oldS, String newS){
        console.appendToConsole("I'm here.");
        String allText = textArea.getText();
        if(!allText.contains(oldS)){
            return false;
        }
        allText = allText.replaceAll(oldS, newS);
        textArea.setText(allText);
        return true;
    }
    
    /**
     * Highlighter tool for Find
     * @param query the search term
     * @return if all instances of the search term were found and highlighted
     */
    public boolean highlightFound(String query){
        
        String allText = textArea.getText();
        if(!allText.contains(query))
            return false;
        else{    
            
            String pattern = query;
            String text = textArea.getText();        
            int index = text.indexOf(pattern);
            while(index >= 0){
                try {                
                    h.addHighlight(index, index + pattern.length(), hPainter);
                    index = text.indexOf(pattern, index + pattern.length());            
                } catch (BadLocationException ex) {
                }
            }
            Highlighter.Highlight[] hilites = h.getHighlights();
            textArea.setCaretPosition(hilites[0].getStartOffset());
            clearHighlightingItem.setEnabled(true);
            return true;
        }
    }
    
    public void moveCaretForward(){
        currItemFound++;
        Highlighter.Highlight[] hilites = h.getHighlights();
        textArea.setCaretPosition(hilites[currItemFound].getStartOffset());
    }
    public void moveCaretBackward(){
        if(currItemFound > 0){
            currItemFound--;
            Highlighter.Highlight[] hilites = h.getHighlights();
            textArea.setCaretPosition(hilites[currItemFound].getStartOffset());
        }
    }
    public void resetCaret(){
        currItemFound = -1;
    }
    
    private void findItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findItemActionPerformed
        // TODO add your handling code here:
        FindFrame findFrame = new FindFrame(this);
        findFrame.setVisible(true);
        findFrame.setResizable(false);
    }//GEN-LAST:event_findItemActionPerformed

    private void clearHighlightingItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearHighlightingItemActionPerformed
        // TODO add your handling code here:
        Highlighter.Highlight[] hilites = h.getHighlights();
        if(hilites.length != 0){
            h.removeAllHighlights();
            clearHighlightingItem.setEnabled(false);
        }
    }//GEN-LAST:event_clearHighlightingItemActionPerformed

    public void clearHighlites(){
        Highlighter.Highlight[] hilites = h.getHighlights();
        if(hilites.length != 0){
            h.removeAllHighlights();
            clearHighlightingItem.setEnabled(false);
        }
    }
    
    private void replaceItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceItemActionPerformed
        // TODO add your handling code here:
        ReplaceFrame replace = new ReplaceFrame(this);
        replace.setVisible(true);
        replace.setResizable(false);
    }//GEN-LAST:event_replaceItemActionPerformed

    private void printItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printItemActionPerformed
        // TODO add your handling code here:
        
        print();
    }//GEN-LAST:event_printItemActionPerformed

    public void print(){
        String defaultPrinter = null;
        try{
            defaultPrinter = PrintServiceLookup.lookupDefaultPrintService().getName();
        }
        catch(NullPointerException e){
            console.appendToConsole("Exception: " +e);
        }
        console.appendToConsole("Print job requested...\nBeginning print job...");
        console.appendToConsole("Default Printer: "+defaultPrinter);
        try {
            boolean complete = textArea.print();
            if (complete) {
                console.appendToConsole("Printing complete.");
            } else {
                console.appendToConsole("Printing cancelled by user.");
            }
        } catch (PrinterException pe) {
            console.appendToConsole("Printing failed: "+pe.getLocalizedMessage());
        }
    }
    
    private void printPreviewItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printPreviewItemActionPerformed
        // TODO add your handling code here:
        
        new PrintPreviewFrame(textArea, this).setVisible(true);
        
        
    }//GEN-LAST:event_printPreviewItemActionPerformed

    private void cutItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutItemActionPerformed
        // TODO add your handling code here:
        
        textArea.cut();
    }//GEN-LAST:event_cutItemActionPerformed

    private void copyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyItemActionPerformed
        // TODO add your handling code here:
        textArea.copy();
    }//GEN-LAST:event_copyItemActionPerformed

    private void pasteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteItemActionPerformed
        // TODO add your handling code here:
        textArea.paste();
    }//GEN-LAST:event_pasteItemActionPerformed

    private void textColorItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textColorItemActionPerformed
        // TODO add your handling code here:
        
        Color c = JColorChooser.showDialog(this, "Select Text Color", textArea.getForeground());
        if(c != null){
            textArea.setForeground(c);
        }
    }//GEN-LAST:event_textColorItemActionPerformed

    private void backColorItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backColorItemActionPerformed
        // TODO add your handling code here:
        
        Color c = JColorChooser.showDialog(this, "Select Background Color", textArea.getBackground());
        if(c != null){
            textArea.setBackground(c);
        }
    }//GEN-LAST:event_backColorItemActionPerformed

    private void consoleItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleItemActionPerformed
        // TODO add your handling code here:
        consoleVisible = !console.isVisible();
        if(consoleVisible){
            int width = this.getWidth() - 13;
            console.setSize(width, console.getHeight());
            int x = this.getX()+5;
            int y = this.getY() + this.getHeight();
            console.setLocation(x, y);
        }
        console.setVisible(consoleVisible);
        if(consoleVisible)
            this.toFront();
    }//GEN-LAST:event_consoleItemActionPerformed

    private void wordCountItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wordCountItemActionPerformed
        // TODO add your handling code here:
        CountFrame count = new CountFrame(this, textArea);
        count.setVisible(true);
        count.setResizable(false);
    }//GEN-LAST:event_wordCountItemActionPerformed

    private void wordWrapItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wordWrapItemActionPerformed
        // TODO add your handling code here:
        boolean selected = wordWrapItem.getState();
        textArea.setLineWrap(selected);
        textArea.setWrapStyleWord(selected);
        wordWrapPopupItem.setState(selected);
    }//GEN-LAST:event_wordWrapItemActionPerformed

    private void highlighterColorItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highlighterColorItemActionPerformed
        // TODO add your handling code here:
        Color chosen = JColorChooser.showDialog(this, "Set Highlighter Color", hPainter.getColor());
        hPainter = new DefaultHighlighter.DefaultHighlightPainter(chosen); 
    }//GEN-LAST:event_highlighterColorItemActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        // TODO add your handling code here:
        if(consoleVisible){
            int width = this.getWidth() - 13;
            console.setSize(width, console.getHeight());
            int x = this.getX()+5;
            int y = this.getY() + this.getHeight();
            console.setLocation(x, y);
            console.revalidate();
        }
    }//GEN-LAST:event_formComponentResized

    private void formComponentMoved(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentMoved
        // TODO add your handling code here:
        if(consoleVisible){
            int width = this.getWidth() - 13;
            console.setSize(width, console.getHeight());
            int x = this.getX()+5;
            int y = this.getY() + this.getHeight();
            console.setLocation(x, y);
            console.revalidate();
        }
    }//GEN-LAST:event_formComponentMoved

    private void formWindowDeiconified(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowDeiconified
        // TODO add your handling code here:
        if(consoleVisible)
            console.setVisible(true);
        destroyTray();
        this.setVisible(true);
    }//GEN-LAST:event_formWindowDeiconified

    private void formWindowIconified(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowIconified
        // TODO add your handling code here:
        if(consoleVisible)
            console.setVisible(false);
        setUpTray();
        this.setVisible(false);
        if(!wasIconified){
            trayIcon.displayMessage("JNotepad", "JNotepad is still running! "
                    + "Right click tray icon to close.", TrayIcon.MessageType.INFO);
            wasIconified = true;
        }
    }//GEN-LAST:event_formWindowIconified

    private void editorModeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editorModeItemActionPerformed
        // TODO add your handling code here:
        editorMode = !editorMode;
        lineNumsField.setVisible(editorMode);
        lineNumsScroll.setVisible(editorMode);
        this.revalidate();
        if(editorMode) {
            setUpLineNums();
            addRunMenu();
        }
        else
            menuBar.remove(5);
    }//GEN-LAST:event_editorModeItemActionPerformed
    
    private void addRunMenu(){
        JMenu runMenu = new JMenu("Run");
        JMenuItem runItem = new JMenuItem("Build and Run");
        runItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent ae){
                if(currentFileOpen == null)
                    saveAs();
                runCode();
            }
        });
        runMenu.add(runItem);
        menuBar.add(runMenu);
    }
    
    // TODO: write this code
    private void runCode(){
        if(currentFileOpen == null)
            return;
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        System.out.println("About to compile...");
        int compilationResult = compiler.run(null, System.out, null, currentFileOpen.getAbsolutePath());
        if(compilationResult == 0)
            System.out.println("Compilation successful");
        else
            System.err.println("Compilation failed");
    }
    
    private void setUpLineNums(){
        lineNumsField.setText("");
        for(int i=0; i<textArea.getLineCount(); i++){
            lineNumsField.append(""+(i+1)+"\n");
        }
    }
    
    class CustomFilter extends javax.swing.filechooser.FileFilter {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getAbsolutePath().endsWith(".txt");
        }
        @Override
        public String getDescription() {
            return "Text Documents (*.txt)";
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
        try {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Test");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JavaNotepad.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new JavaNotepad().setVisible(true);
            }
        });
    }
    
    private File currentFileOpen;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JMenuItem backColorItem;
    private static javax.swing.JMenuItem clearHighlightingItem;
    private javax.swing.JMenuItem clearTextItem;
    private javax.swing.JMenuItem consoleItem;
    private javax.swing.JMenuItem copyItem;
    private javax.swing.JMenuItem cutItem;
    private javax.swing.JMenu developerMenu;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBoxMenuItem editorModeItem;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel fileOpenLabel;
    private javax.swing.JLabel fileWriteLabel;
    private javax.swing.JMenuItem findItem;
    private javax.swing.JMenuItem fontItem;
    private javax.swing.JMenu formatMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem highlighterColorItem;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea lineNumsField;
    private javax.swing.JScrollPane lineNumsScroll;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openFileItem;
    private javax.swing.JMenuItem pasteItem;
    private javax.swing.JMenuItem printItem;
    private javax.swing.JMenuItem printPreviewItem;
    private javax.swing.JMenuItem replaceItem;
    private javax.swing.JMenuItem saveAsItem;
    private javax.swing.JMenuItem saveItem;
    private javax.swing.JPanel statusPanel;
    private static javax.swing.JTextArea textArea;
    private javax.swing.JMenuItem textColorItem;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JMenuItem wordCountItem;
    private javax.swing.JCheckBoxMenuItem wordWrapItem;
    // End of variables declaration//GEN-END:variables
}
