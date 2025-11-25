import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/eventdb";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            e.printStackTrace(); 
            return null;
        }
    }
}


class CustomerDAO {
    public int saveCustomer(String name, String phone, String address) throws Exception {
        String sql = "INSERT INTO customers (name, phone, address) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) throw new Exception("JDBC Not Connected");
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, phone);
                ps.setString(3, address);
                int affected = ps.executeUpdate();
                if (affected == 0) throw new Exception("Insert failed, no rows affected.");
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                    else return -1;
                }
            }
        }
    }

    public void saveBooking(int custId, String type, String date, String time, double price, String venue, String extra) throws Exception {
        String sql = "INSERT INTO bookings (customer_id, event_type, event_date, event_time, price, venue, extra_details) VALUES (?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) throw new Exception("JDBC Not Connected");
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (custId > 0) ps.setInt(1, custId);
                else ps.setNull(1, Types.INTEGER);
                ps.setString(2, type);
                ps.setString(3, date);
                ps.setString(4, time);
                ps.setDouble(5, price);
                ps.setString(6, venue);
                ps.setString(7, extra);
                ps.executeUpdate();
            }
        }
    }
}


abstract class Event {
    private String date;
    private String time;
    private String venue;

    public void setDetails(String d, String t, String v) { this.date = d; this.time = t; this.venue = v; }
    public String getDate(){ return date; }
    public String getTime(){ return time; }
    public String getVenue(){ return venue; }
    public abstract double calculatePrice();
    public String extraDetails(){ return ""; } 
}


class MarriageEvent extends Event {
    private String bride, groom;
    public void setBride(String b){ this.bride = b; }
    public void setGroom(String g){ this.groom = g; }
    public String getBride(){ return bride; }
    public String getGroom(){ return groom; }
    public double calculatePrice() { return 50000; }
    public String extraDetails(){ return "Bride: "+bride+", Groom: "+groom; }
}

class BirthdayEvent extends Event {
    private String birthdayName;
    public void setBirthdayName(String n){ this.birthdayName = n; }
    public String getBirthdayName(){ return birthdayName; }
    public double calculatePrice() { return 15000; }
    public String extraDetails(){ return "Birthday Name: "+birthdayName; }
}

class EngagementEvent extends Event {
    private String coupleNames, venuePreference;
    private int guestCount;
    public void setCoupleNames(String s){ this.coupleNames = s; }
    public void setVenuePreference(String v){ this.venuePreference = v; }
    public void setGuestCount(int g){ this.guestCount = g; }
    public String getCoupleNames(){ return coupleNames; }
    public double calculatePrice() { return 30000 + guestCount*500; }
    public String extraDetails(){ return "Couple: "+coupleNames+", Guests: "+guestCount; }
}

class BabyShowerEvent extends Event {
    private String motherName, theme;
    private int guestCount;
    public void setMotherName(String m){ this.motherName = m; }
    public void setTheme(String t){ this.theme = t; }
    public void setGuestCount(int g){ this.guestCount = g; }
    public double calculatePrice() { return 20000 + guestCount*300; }
    public String extraDetails(){ return "Mother: "+motherName+", Theme: "+theme+", Guests: "+guestCount; }
}

class AnniversaryEvent extends Event {
    private String coupleNames, venueType;
    private int yearsCompleted;
    public void setCoupleNames(String s){ this.coupleNames = s; }
    public void setVenueType(String v){ this.venueType = v; }
    public void setYearsCompleted(int y){ this.yearsCompleted = y; }
    public double calculatePrice() { return 25000 + yearsCompleted*1000; }
    public String extraDetails(){ return "Couple: "+coupleNames+", Years: "+yearsCompleted+", Venue: "+venueType; }
}


class PaymentProcessor {
    public synchronized void processPayment(double amount) throws Exception {
        if(amount <= 0) throw new Exception("Invalid Amount");
        // simulate processing
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}


class PlaceholderTextField extends JTextField {
    private String placeholder;
    private Color placeholderColor = Color.GRAY;
    private boolean showPlaceholderAlways = false;

    public PlaceholderTextField(String placeholder) {
        super();
        this.placeholder = placeholder;
      
        setOpaque(true);
        
        setMargin(new Insets(2,6,2,2));
    }

    public PlaceholderTextField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder;
        setOpaque(true);
        setMargin(new Insets(2,6,2,2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if ((getText() == null || getText().length() == 0) && ! (showPlaceholderAlways && getText()!=null)) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(placeholderColor);
            Font prev = g2.getFont();
            g2.setFont(prev.deriveFont(Font.ITALIC));
            Insets ins = getInsets();
            int y = getHeight() - ins.bottom - 6;
            g2.drawString(placeholder, ins.left, y);
            g2.setFont(prev);
            g2.dispose();
        }
    }

    public void setPlaceholderColor(Color c){ this.placeholderColor = c; }
    public void setShowPlaceholderAlways(boolean v){ this.showPlaceholderAlways = v; }
}

class ValidationUtils {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("hh:mm a");

    static {
        DATE_FMT.setLenient(false);
        TIME_FMT.setLenient(false);
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{7,15}");
    }

    public static boolean isValidDate(String dateStr) {
        if (dateStr == null) return false;
        try { DATE_FMT.parse(dateStr.trim()); return true; }
        catch (ParseException e) { return false; }
    }

    public static boolean isValidTime(String timeStr) {
        if (timeStr == null) return false;
        try { TIME_FMT.parse(timeStr.trim()); return true; }
        catch (ParseException e) { return false; }
    }
}

class CustomerForm extends JFrame {
    public CustomerForm() {
        setTitle("Customer Details"); setSize(520,420);
        setLayout(null); setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel header = new JLabel("Enter Customer Details", SwingConstants.CENTER);
        header.setBounds(60,10,400,30); header.setFont(header.getFont().deriveFont(18f));

        JLabel l1 = new JLabel("Name:"), l2 = new JLabel("Phone:"), l3 = new JLabel("Address:");
        JTextField t1 = new JTextField(), t2 = new JTextField(), t3 = new JTextField();
        JButton next = new JButton("Next"); next.setBounds(200,310,120,40);

        l1.setBounds(60,80,150,30); t1.setBounds(220,80,240,30);
        l2.setBounds(60,130,150,30); t2.setBounds(220,130,240,30);
        l3.setBounds(60,180,150,30); t3.setBounds(220,180,240,80);

        add(header); add(l1); add(t1); add(l2); add(t2); add(l3); add(t3); add(next);

        next.addActionListener(e -> {
            String name = t1.getText().trim();
            String phone = t2.getText().trim();
            String address = t3.getText().trim();

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidPhone(phone)) {
                JOptionPane.showMessageDialog(this, "Enter valid phone number (7-15 digits).", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int custId = -1;
            try {
                CustomerDAO dao = new CustomerDAO();
                custId = dao.saveCustomer(name, phone, address);
            } catch(Exception ex) {
                
                JOptionPane.showMessageDialog(this,"⚠ JDBC not connected or error saving customer. Proceeding without DB save.","Warning",JOptionPane.WARNING_MESSAGE);
            }

            EventSelection evSel = new EventSelection(custId);
            evSel.setVisible(true);
            setVisible(false);
        });
    }
}


class EventSelection extends JFrame {
    int custId;
    public EventSelection(int id) {
        custId = id;
        setTitle("Select Event"); setSize(520,640); setLayout(null); setLocationRelativeTo(null);

        JLabel header = new JLabel("Select which event you want to organise", SwingConstants.CENTER);
        header.setBounds(60,10,400,30); header.setFont(header.getFont().deriveFont(16f));
        add(header);

        String[] events = {"Marriage","Birthday Party","Engagement Ceremony","Baby Shower","Anniversary Celebration"};
        for(int i=0;i<events.length;i++){
            JButton b = new JButton(events[i]); b.setBounds(160,60+i*95,200,60); b.setFont(b.getFont().deriveFont(14f));
            add(b);
            int idx=i;
            b.addActionListener(e -> {
                switch(idx){
                    case 0: new MarriageForm(custId,this).setVisible(true); break;
                    case 1: new BirthdayForm(custId,this).setVisible(true); break;
                    case 2: new EngagementForm(custId,this).setVisible(true); break;
                    case 3: new BabyShowerForm(custId,this).setVisible(true); break;
                    case 4: new AnniversaryForm(custId,this).setVisible(true); break;
                }
                setVisible(false);
            });
        }
    }
}


abstract class EventForm extends JFrame {
    int custId; EventSelection prevPage;
    EventForm(int custId, EventSelection prevPage) {
        this.custId=custId; this.prevPage=prevPage;
        setSize(560,520); setLayout(null); setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null);
    }
}


class MarriageForm extends EventForm {
    public MarriageForm(int custId, EventSelection prevPage){
        super(custId,prevPage); setTitle("Marriage Event");

        JLabel header = new JLabel("Marriage Event Details",SwingConstants.CENTER);
        header.setBounds(60,10,440,30); header.setFont(header.getFont().deriveFont(16f));

        JLabel l1=new JLabel("Bride Name:"), l2=new JLabel("Groom Name:"), l3=new JLabel("Venue:");
        JLabel l4=new JLabel("Date:"), l5=new JLabel("Time:");

        JTextField tb=new JTextField(), tg=new JTextField(), tvenue=new JTextField();
        PlaceholderTextField tdate = new PlaceholderTextField("DD/MM/YYYY");
        PlaceholderTextField ttime = new PlaceholderTextField("HH:MM AM/PM");

        tdate.setBounds(260,260,180,30);
        ttime.setBounds(260,320,180,30);

        JButton confirm=new JButton("Confirm"), back=new JButton("Back");
        confirm.setBounds(140,400,120,50); back.setBounds(310,400,120,50);

        l1.setBounds(60,70,150,30); tb.setBounds(260,70,180,30);
        l2.setBounds(60,120,150,30); tg.setBounds(260,120,180,30);
        l3.setBounds(60,170,150,30); tvenue.setBounds(260,170,180,30);
        l4.setBounds(60,260,150,30);
        l5.setBounds(60,320,150,30);

        add(header); add(l1); add(tb); add(l2); add(tg); add(l3); add(tvenue);
        add(l4); add(tdate); add(l5); add(ttime); add(confirm); add(back);

        confirm.addActionListener(e -> {
            String bride = tb.getText().trim();
            String groom = tg.getText().trim();
            String venue = tvenue.getText().trim();
            String date = tdate.getText().trim();
            String time = ttime.getText().trim();

            if (bride.isEmpty() || groom.isEmpty() || venue.isEmpty() || date.isEmpty() || time.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidDate(date)) {
                JOptionPane.showMessageDialog(this, "Enter valid date in DD/MM/YYYY format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidTime(time)) {
                JOptionPane.showMessageDialog(this, "Enter valid time in HH:MM AM/PM format (e.g. 07:30 PM).", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            MarriageEvent ev=new MarriageEvent();
            ev.setBride(bride); ev.setGroom(groom);
            ev.setDetails(date,time,venue);

            try {
                if(custId!=-1) new CustomerDAO().saveBooking(custId,"Marriage",ev.getDate(),ev.getTime(),ev.calculatePrice(),ev.getVenue(),ev.extraDetails());
            } catch(Exception ex) {
                JOptionPane.showMessageDialog(this,"⚠ JDBC not connected or error saving booking. Booking not saved in DB.","Warning",JOptionPane.WARNING_MESSAGE);
            }

            new PaymentPage(custId,"Marriage",ev,prevPage).setVisible(true);
            dispose();
        });

        back.addActionListener(e -> { prevPage.setVisible(true); dispose(); });
    }
}


class BirthdayForm extends EventForm {
    public BirthdayForm(int custId, EventSelection prevPage){
        super(custId,prevPage); setTitle("Birthday Party");

        JLabel header = new JLabel("Birthday Party Details",SwingConstants.CENTER);
        header.setBounds(60,10,440,30); header.setFont(header.getFont().deriveFont(16f));

        JLabel l0=new JLabel("Birthday Name:"), l1=new JLabel("Venue:");
        JLabel l2=new JLabel("Date:"), l3=new JLabel("Time:");

        JTextField tb=new JTextField(), tvenue=new JTextField();
        PlaceholderTextField tdate = new PlaceholderTextField("DD/MM/YYYY");
        PlaceholderTextField ttime = new PlaceholderTextField("HH:MM AM/PM");

        l0.setBounds(60,70,150,30); tb.setBounds(260,70,180,30);
        l1.setBounds(60,120,150,30); tvenue.setBounds(260,120,180,30);
        l2.setBounds(60,200,150,30); tdate.setBounds(260,200,180,30);
        l3.setBounds(60,260,150,30); ttime.setBounds(260,260,180,30);

        JButton confirm=new JButton("Confirm"), back=new JButton("Back");
        confirm.setBounds(140,360,120,50); back.setBounds(310,360,120,50);

        add(header); add(l0); add(tb); add(l1); add(tvenue);
        add(l2); add(tdate); add(l3); add(ttime); add(confirm); add(back);

        confirm.addActionListener(e -> {
            String name = tb.getText().trim(); String venue = tvenue.getText().trim();
            String date = tdate.getText().trim(); String time = ttime.getText().trim();

            if (name.isEmpty() || venue.isEmpty() || date.isEmpty() || time.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidDate(date)) {
                JOptionPane.showMessageDialog(this, "Enter valid date in DD/MM/YYYY format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidTime(time)) {
                JOptionPane.showMessageDialog(this, "Enter valid time in HH:MM AM/PM format (e.g. 07:30 PM).", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            BirthdayEvent ev=new BirthdayEvent();
            ev.setBirthdayName(name); ev.setDetails(date,time,venue);

            try { if(custId!=-1) new CustomerDAO().saveBooking(custId,"Birthday Party",ev.getDate(),ev.getTime(),ev.calculatePrice(),ev.getVenue(),ev.extraDetails()); }
            catch(Exception ex) { JOptionPane.showMessageDialog(this,"⚠ JDBC not connected or error saving booking. Booking not saved in DB.","Warning",JOptionPane.WARNING_MESSAGE); }

            new PaymentPage(custId,"Birthday Party",ev,prevPage).setVisible(true);
            dispose();
        });

        back.addActionListener(e -> { prevPage.setVisible(true); dispose(); });
    }
}

class EngagementForm extends EventForm {
    public EngagementForm(int custId, EventSelection prevPage){
        super(custId,prevPage); setTitle("Engagement Ceremony");

        JLabel header = new JLabel("Engagement Ceremony Details",SwingConstants.CENTER);
        header.setBounds(60,10,440,30); header.setFont(header.getFont().deriveFont(16f));

        JLabel l1=new JLabel("Couple Names:"), l2=new JLabel("Venue Preference:"), l3=new JLabel("Guest Count:");
        JLabel l4=new JLabel("Date:"), l5=new JLabel("Time:");

        JTextField t1=new JTextField(), t2=new JTextField(), t3=new JTextField();
        PlaceholderTextField tdate = new PlaceholderTextField("DD/MM/YYYY");
        PlaceholderTextField ttime = new PlaceholderTextField("HH:MM AM/PM");

        l1.setBounds(60,70,150,30); t1.setBounds(260,70,180,30);
        l2.setBounds(60,120,150,30); t2.setBounds(260,120,180,30);
        l3.setBounds(60,170,150,30); t3.setBounds(260,170,180,30);
        l4.setBounds(60,230,150,30); tdate.setBounds(260,230,180,30);
        l5.setBounds(60,290,150,30); ttime.setBounds(260,290,180,30);

        JButton confirm=new JButton("Confirm"), back=new JButton("Back");
        confirm.setBounds(140,380,120,50); back.setBounds(310,380,120,50);

        add(header); add(l1); add(t1); add(l2); add(t2); add(l3); add(t3);
        add(l4); add(tdate); add(l5); add(ttime);
        add(confirm); add(back);

        confirm.addActionListener(e -> {
            String couple = t1.getText().trim(); String venuePref = t2.getText().trim();
            String gText = t3.getText().trim();
            String date = tdate.getText().trim(); String time = ttime.getText().trim();

            if (couple.isEmpty() || venuePref.isEmpty() || gText.isEmpty() || date.isEmpty() || time.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int guests;
            try { guests = Integer.parseInt(gText); if (guests < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Guest count must be a non-negative integer.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidDate(date)) {
                JOptionPane.showMessageDialog(this, "Enter valid date in DD/MM/YYYY format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidTime(time)) {
                JOptionPane.showMessageDialog(this, "Enter valid time in HH:MM AM/PM format (e.g. 07:30 PM).", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            EngagementEvent ev = new EngagementEvent();
            ev.setCoupleNames(couple); ev.setVenuePreference(venuePref); ev.setGuestCount(guests);
            ev.setDetails(date,time,venuePref);

            try { if(custId!=-1) new CustomerDAO().saveBooking(custId,"Engagement Ceremony",ev.getDate(),ev.getTime(),ev.calculatePrice(),ev.getVenue(),ev.extraDetails()); }
            catch(Exception ex) { JOptionPane.showMessageDialog(this,"⚠ JDBC not connected or error saving booking. Booking not saved in DB.","Warning",JOptionPane.WARNING_MESSAGE); }

            new PaymentPage(custId,"Engagement Ceremony",ev,prevPage).setVisible(true);
            dispose();
        });

        back.addActionListener(e -> { prevPage.setVisible(true); dispose(); });
    }
}


class BabyShowerForm extends EventForm {
    public BabyShowerForm(int custId, EventSelection prevPage){
        super(custId,prevPage); setTitle("Baby Shower");

        JLabel header = new JLabel("Baby Shower Details",SwingConstants.CENTER);
        header.setBounds(60,10,440,30); header.setFont(header.getFont().deriveFont(16f));

        JLabel l1=new JLabel("Mother's Name:"), l2=new JLabel("Theme:"), l3=new JLabel("Guest Count:");
        JLabel l4=new JLabel("Date:"), l5=new JLabel("Time:");

        JTextField t1=new JTextField(), t2=new JTextField(), t3=new JTextField();
        PlaceholderTextField tdate = new PlaceholderTextField("DD/MM/YYYY");
        PlaceholderTextField ttime = new PlaceholderTextField("HH:MM AM/PM");

        l1.setBounds(60,70,150,30); t1.setBounds(260,70,180,30);
        l2.setBounds(60,120,150,30); t2.setBounds(260,120,180,30);
        l3.setBounds(60,170,150,30); t3.setBounds(260,170,180,30);
        l4.setBounds(60,230,150,30); tdate.setBounds(260,230,180,30);
        l5.setBounds(60,290,150,30); ttime.setBounds(260,290,180,30);

        JButton confirm=new JButton("Confirm"), back=new JButton("Back");
        confirm.setBounds(140,380,120,50); back.setBounds(310,380,120,50);

        add(header); add(l1); add(t1); add(l2); add(t2); add(l3); add(t3);
        add(l4); add(tdate); add(l5); add(ttime); add(confirm); add(back);

        confirm.addActionListener(e -> {
            String mother = t1.getText().trim(), theme = t2.getText().trim(), gText = t3.getText().trim();
            String date = tdate.getText().trim(), time = ttime.getText().trim();

            if (mother.isEmpty() || theme.isEmpty() || gText.isEmpty() || date.isEmpty() || time.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int guests;
            try { guests = Integer.parseInt(gText); if (guests < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Guest count must be a non-negative integer.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidDate(date)) {
                JOptionPane.showMessageDialog(this, "Enter valid date in DD/MM/YYYY format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidTime(time)) {
                JOptionPane.showMessageDialog(this, "Enter valid time in HH:MM AM/PM format (e.g. 07:30 PM).", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            BabyShowerEvent ev = new BabyShowerEvent();
            ev.setMotherName(mother); ev.setTheme(theme); ev.setGuestCount(guests);
            ev.setDetails(date,time,"");

            try { if(custId!=-1) new CustomerDAO().saveBooking(custId,"Baby Shower",ev.getDate(),ev.getTime(),ev.calculatePrice(),ev.getVenue(),ev.extraDetails()); }
            catch(Exception ex) { JOptionPane.showMessageDialog(this,"⚠ JDBC not connected or error saving booking. Booking not saved in DB.","Warning",JOptionPane.WARNING_MESSAGE); }

            new PaymentPage(custId,"Baby Shower",ev,prevPage).setVisible(true);
            dispose();
        });

        back.addActionListener(e -> { prevPage.setVisible(true); dispose(); });
    }
}


class AnniversaryForm extends EventForm {
    public AnniversaryForm(int custId, EventSelection prevPage){
        super(custId,prevPage); setTitle("Anniversary Celebration");

        JLabel header = new JLabel("Anniversary Celebration Details",SwingConstants.CENTER);
        header.setBounds(60,10,440,30); header.setFont(header.getFont().deriveFont(16f));

        JLabel l1=new JLabel("Couple Names:"), l2=new JLabel("Years Completed:"), l3=new JLabel("Venue Type:");
        JLabel l4=new JLabel("Date:"), l5=new JLabel("Time:");

        JTextField t1=new JTextField(), t2=new JTextField(), t3=new JTextField();
        PlaceholderTextField tdate = new PlaceholderTextField("DD/MM/YYYY");
        PlaceholderTextField ttime = new PlaceholderTextField("HH:MM AM/PM");

        l1.setBounds(60,70,150,30); t1.setBounds(260,70,180,30);
        l2.setBounds(60,120,150,30); t2.setBounds(260,120,180,30);
        l3.setBounds(60,170,150,30); t3.setBounds(260,170,180,30);
        l4.setBounds(60,230,150,30); tdate.setBounds(260,230,180,30);
        l5.setBounds(60,290,150,30); ttime.setBounds(260,290,180,30);

        JButton confirm=new JButton("Confirm"), back=new JButton("Back");
        confirm.setBounds(140,380,120,50); back.setBounds(310,380,120,50);

        add(header); add(l1); add(t1); add(l2); add(t2); add(l3); add(t3);
        add(l4); add(tdate); add(l5); add(ttime); add(confirm); add(back);

        confirm.addActionListener(e -> {
            String couple = t1.getText().trim(), yearsText = t2.getText().trim(), venueType = t3.getText().trim();
            String date = tdate.getText().trim(), time = ttime.getText().trim();

            if (couple.isEmpty() || yearsText.isEmpty() || venueType.isEmpty() || date.isEmpty() || time.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int years;
            try { years = Integer.parseInt(yearsText); if (years < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Years completed must be a non-negative integer.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidDate(date)) {
                JOptionPane.showMessageDialog(this, "Enter valid date in DD/MM/YYYY format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidTime(time)) {
                JOptionPane.showMessageDialog(this, "Enter valid time in HH:MM AM/PM format (e.g. 07:30 PM).", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            AnniversaryEvent ev = new AnniversaryEvent();
            ev.setCoupleNames(couple); ev.setYearsCompleted(years); ev.setVenueType(venueType);
            ev.setDetails(date,time,venueType);

            try { if(custId!=-1) new CustomerDAO().saveBooking(custId,"Anniversary Celebration",ev.getDate(),ev.getTime(),ev.calculatePrice(),ev.getVenue(),ev.extraDetails()); }
            catch(Exception ex) { JOptionPane.showMessageDialog(this,"⚠ JDBC not connected or error saving booking. Booking not saved in DB.","Warning",JOptionPane.WARNING_MESSAGE); }

            new PaymentPage(custId,"Anniversary Celebration",ev,prevPage).setVisible(true);
            dispose();
        });

        back.addActionListener(e -> { prevPage.setVisible(true); dispose(); });
    }
}


class PaymentPage extends JFrame {


public PaymentPage(int custId, String eventName, Event ev, EventSelection prevPage) {
setTitle("Payment");
setSize(520,500);
setLayout(null);
setLocationRelativeTo(null);
setDefaultCloseOperation(EXIT_ON_CLOSE);


JLabel header = new JLabel("Payment Summary", SwingConstants.CENTER);
header.setBounds(60,10,400,40);
header.setFont(header.getFont().deriveFont(18f));
add(header);


JTextArea details = new JTextArea();
details.setEditable(false);
details.setFont(new Font("Arial", Font.PLAIN, 15));


StringBuilder sb = new StringBuilder();
sb.append("Event Type: ").append(eventName).append("\n");
sb.append("Date: ").append(ev.getDate()).append("\n");
sb.append("Time: ").append(ev.getTime()).append("\n");
sb.append("Venue: ").append(ev.getVenue()).append("\n\n");
sb.append("Extra Details:\n").append(ev.extraDetails()).append("\n\n");
sb.append("Total Amount to Pay: ₹").append(ev.calculatePrice()).append("\n");


details.setText(sb.toString());


JScrollPane pane = new JScrollPane(details);
pane.setBounds(60,70,400,260);
add(pane);


JButton payNow = new JButton("Pay Now");
payNow.setBounds(160,360,180,50);
add(payNow);


payNow.addActionListener(e -> {
double amount = ev.calculatePrice();
PaymentProcessor pp = new PaymentProcessor();


try {
pp.processPayment(amount);


JOptionPane.showMessageDialog(
this,
"Payment Successful!\nPaid Amount: ₹" + amount +
"\n\n🎉 Booking Confirmed! 🎉",
"Success",
JOptionPane.INFORMATION_MESSAGE
);


prevPage.setVisible(true);
dispose();


} catch (Exception ex) {
JOptionPane.showMessageDialog(
this,
"Payment Failed: " + ex.getMessage(),
"Error",
JOptionPane.ERROR_MESSAGE
);
}
});
}
}


public class EventManagementApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {

                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new CustomerForm().setVisible(true);
        });
    }
}