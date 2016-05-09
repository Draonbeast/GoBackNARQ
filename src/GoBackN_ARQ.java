
import java.applet.Applet;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

public class GoBackN_ARQ extends Applet implements ActionListener, Runnable {

    final int window = 5;    // Default values of parameters
    final int packet_width = 20;//Obvious name
    final int packet_height = 30;//Another obvious name
    final int horizontal_offset = 100;//Float everything this far into the "Center"
    final int vertical_offset = 50;//Make room for buttons
    final int vertical_clearance = 300;//Space down to top of Sender Row
    final int max_packets = 20;//Space to test without resetting,
                                // Couldn't figure out how to easily loop back to the beginning
    final int time_out_sec = 30;//Static amount to wait. Sorry it's so long


    //COLORS with obvious names for usability
    final Color unack_color = Color.blue;  //  Default colors of different packets and Sender/Receiver Frames
    final Color destination_color = Color.blue;
    final Color roam_pack_color = Color.red;
    final Color roam_ack_color = Color.orange;
    final Color ack_returned = Color.green;

    //To be initialized in init()
    int base, next_packet, speed;
    boolean timerFlag, Sleep_flag;
    Button send_btn, stop_btn, speed_up_btn, slow_down_btn, reset_btn;
    /**
     * Threads to do things.
     * There can be multiple of the same
     *          Confusing I know
     */
    Thread gbnThread;
    Thread timerThread;

    Dimension dimension;    // Size stuff
    Image image;            //Needs a display right?
    Graphics graphics;      //Need to display stuff right?

    String message;//To communicate with the user

    Packet packets[];//Need to keep track of them somehow, obvious name is obvious

    /**
     * Needed for implementation of runnable
     * Initialize all the things
     * Base variables for counting
     * Allocate space for packets (I guess not really allocating. Java is nice like that)
     * and Buttons
     * yay buttons
     */
    public void init() {

        base = 0;    // initialize start point
        next_packet = 0;   //Defining next to send_btn
        speed = 5;    //Defining speed.

        packets = new Packet[max_packets];
        message = "Press 'Send New' button to start.";

        // Defining the buttons
        send_btn = new Button("Send New");
        send_btn.setActionCommand("send");  //Thought about maaking it automatic to send
        send_btn.addActionListener(this);   // Decided against it

        stop_btn = new Button("Stop");
        stop_btn.setActionCommand("stop");
        stop_btn.addActionListener(this);

        speed_up_btn = new Button("Faster");
        speed_up_btn.setActionCommand("speed_up_btn");
        speed_up_btn.addActionListener(this);

        slow_down_btn = new Button("Slower");
        slow_down_btn.setActionCommand("slow_down_btn");
        slow_down_btn.addActionListener(this);

        reset_btn = new Button("Reset");
        reset_btn.setActionCommand("reset");
        reset_btn.addActionListener(this);

  // Adding the buttons to the applet Position not specified
        add(send_btn);
        add(stop_btn);
        add(speed_up_btn);
        add(slow_down_btn);
        add(reset_btn);
    }

    public void start() {
        if (gbnThread == null) // Creating main thread and start it 
        {
            gbnThread = new Thread(this);
        }
        gbnThread.start();
    }

    @Override
    public void run() {
        Thread currenthread = Thread.currentThread();

        while (currenthread == gbnThread) // While the animation is running 
        {
            if (inTransit(packets)) // Checks if any of the packets are travelling
            {
                for (int i = 0; i < max_packets; i++) {
                    if (packets[i] != null) {
                        if (packets[i].on_way) // If packet is roaming
                        {
                            if (packets[i].packet_pos > packet_height +5) {
                                packets[i].packet_pos -= 5;  // Move packet + or minus would specify direction
                                                                //It's funny when it runs off the screen
                            } else if (packets[i].packet_ack) // If it is moving to destination_color
                            {
                                packets[i].reached_dest = true;
                                if (check_upto_n(i)) // Send acknowledgement if all preceeding
                                {       // packets are received.
                                    packets[i].packet_pos = vertical_clearance - packet_height;
                                    packets[i].packet_ack = false;
                                    message = "Packet " + i + " received. Acknowledge sent.";
                                } else {//Something didn't reach, no ack sent
                                    packets[i].on_way = false;
                                    message = "Packet " + i + " received. No acknowledge sent.";

                                }
                            } else if (!packets[i].packet_ack) // receipt of acknowledgement
                            {
                                message = "Packet " + i + " acknowledge received.";
                                packets[i].on_way = false;
                                for (int n = 0; n <= i; n++) {
                                    packets[n].acknowledged = true;
                                }

                                timerThread = null;    //resetting timer thread

                                if (i + window < max_packets) {
                                    base = i + 1;
                                }
                                if (next_packet < base + window) {//Space to send more packets enable button
                                    send_btn.setEnabled(true);
                                }

                                if (base != next_packet) {
                                    message += " Timer restarted.";
                                    timerThread = new Thread(this);
                                    Sleep_flag = true;
                                    timerThread.start();
                                } else {
                                    message += " Timer stopped.";
                                }
                            }
                        }
                    }
                }
                /***
                 *  End of scope of For loop
                 *  Within this is the main protocol for GoBackN ARQ
                 */
                repaint();

                try {
                    Thread.sleep(1000 / speed);
                } catch (InterruptedException e) {
                    System.out.println("Help");
                }
            } else {//
                gbnThread = null;
            }
        }

        while (currenthread == timerThread) {
            if (Sleep_flag) {
                Sleep_flag = false;
                try {
                    Thread.sleep(time_out_sec * 650);//Couldn't get the timer to be based off of speed.
                } catch (InterruptedException e) {
                    System.out.println("Timer interrupted.");
                }
            } else {
                for (int n = base; n < base + window; n++) {
                    if (packets[n] != null) {
                        if (!packets[n].acknowledged) {
                            packets[n].on_way = true;
                            packets[n].packet_ack = true;
                            packets[n].packet_pos =  vertical_clearance - packet_height;
                        }
                    }
                }
                Sleep_flag = true;
                if (gbnThread == null) {
                    gbnThread = new Thread(this);
                    gbnThread.start();
                }

                message = "Packets resent by timer. Timer restarted.";
            }
        }
    }//Close the rest of the scopes for Run()

    //Deprecated mouseDown function. Couldn't find applicable updated one
    public boolean mouseDown(Event e, int x, int y) {
        int i, xpos, ypos;
        i = (x - horizontal_offset) / (packet_width + 7);
        if (packets[i] != null) {
            xpos = horizontal_offset + (packet_width + 7) * i;
            ypos = packets[i].packet_pos;

            if (x >= xpos && x <= xpos + packet_width && packets[i].on_way) {
                //Loooong If to check position of mouse click
                if ((packets[i].packet_ack && y >= vertical_offset + ypos && y <= vertical_offset + ypos + packet_height)
                        || ((!packets[i].packet_ack) && y >= vertical_offset + vertical_clearance - ypos && y <= vertical_offset + vertical_clearance - ypos + packet_height)) {
                    message = "Packet " + i + " destroyed.";
                    packets[i].on_way = false;
                    repaint();
                } else {
                    message = "Click on a moving packet to select.";
                }
            } else {
                message = "Click on a moving packet to select.";
            }
        }
        return true;
    }
    /**
     * Function for handling all user events
     * Send button
    */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd == "send" && next_packet < base + window) // Send button is pressed
        {
            packets[next_packet] = new Packet(true, vertical_clearance - packet_height);

            message = "Packet " + next_packet + " sent.";

            if (base == next_packet) {    // i.e. the window is empty and a new data is getting in
                message += " Timer set for packet " + base + ".";
                if (timerThread == null) {
                    timerThread = new Thread(this);
                }
                Sleep_flag = true;
                timerThread.start();
            }

            repaint();
            next_packet++;
            if (next_packet == base + window) {//No more space to display packets Disable button
                send_btn.setEnabled(false);     //Lots of blue on top and green on the bottom
            }
            start();
        } else if (cmd == "speed_up_btn") // Faster button pressed
        {
            speed += 2;
            message = "Simulation speed increased by 2";
        } else if (cmd == "slow_down_btn" && speed > 2) {
            speed -= 2;
            message = "Simulation speed decreased by 2";
        } else if (cmd == "stop") {
            gbnThread = null;
            if (timerThread != null) {
                timerFlag = true;
                timerThread = null;
            }
            stop_btn.setLabel("Start");
            stop_btn.setActionCommand("start");

            // disabling all the buttons
            send_btn.setEnabled(false);
            slow_down_btn.setEnabled(false);
            speed_up_btn.setEnabled(false);

            message = "Simulation paused.";
            repaint();
        } else if (cmd == "start") {//Push the start button
            message = "Simulation resumed.";
            stop_btn.setLabel("Stop");
            stop_btn.setActionCommand("stop");
            if (timerFlag) {
                message += " Timer running.";
                timerThread = new Thread(this);
                Sleep_flag = true;
                timerThread.start();
            }

            // enabling the buttons 
            send_btn.setEnabled(true);
            slow_down_btn.setEnabled(true);
            speed_up_btn.setEnabled(true);
            repaint();
            /**
             * repaint to show new messages
             * */
            start();//Starts main thread again so things will move
        }  else if (cmd == "reset") {
            reset();
        }
    }



    public void paint(Graphics g)
    {
        update(g);
    }

    /**
     * All of this happens at whatever rate the thread runs
     * Not really sure how fast that is, but much too fast to see anything happen
     * Sleep slows it down
     * function is 1000 / speed(int)
     */


    public void update(Graphics g) {
        Dimension d = getSize();
        Font base_font = g.getFont();
        Font temp_font = g.getFont().deriveFont(Font.BOLD);//obvious name is obvious


        //Create the graphics context
        if ((graphics == null) || (d.width != dimension.width) || (d.height != dimension.height)) {
            dimension = d;
            image = createImage(1100, 550);
            graphics = image.getGraphics();
        }

        //Erase the previous image. 
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, d.width, d.height);

  //Drawing sliding window
        graphics.setColor(Color.black);
        graphics.draw3DRect(horizontal_offset + base * (packet_width + 7) - 4, vertical_clearance + vertical_offset - 4, (window) * (packet_width + 7) + 1, packet_height + 7, true);

        for (int i = 0; i < max_packets; i++) {


            /***********
             * drawing the receiving row
             * Confirms receipt of packets before changing
            *************/

            if (packets[i] == null) {
                graphics.setColor(Color.black);
                graphics.draw3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset, packet_width, packet_height, true);
                graphics.draw3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset + vertical_clearance, packet_width, packet_height, true);
            } else {
                if (packets[i].reached_dest) {
                    graphics.setColor(unack_color);
                    if(check_upto_n(i))//HERE is the CHECK
                    graphics.fill3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset, packet_width, packet_height, true);
                    else //If true ^ fill it, otherwise don't fill it
                        graphics.draw3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset, packet_width, packet_height, true);
                } else {//Didn't reach yet, still don't fill
                    graphics.setColor(destination_color);
                    graphics.draw3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset, packet_width, packet_height, true);
                }


    // drawing the sending row

                if (packets[i].acknowledged) {
                    graphics.setColor(ack_returned);
                    graphics.fill3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset + vertical_clearance, packet_width, packet_height, true);
                } else {
                    graphics.setColor(roam_pack_color);
                    graphics.draw3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset + vertical_clearance, packet_width, packet_height, true);
                }

    // drawing the roaming packets
                if (packets[i].on_way) {
                    if (packets[i].packet_ack) {
                        graphics.setColor(roam_pack_color);
                    } else {
                        graphics.setColor(roam_ack_color);
                    }

                    if (packets[i].packet_ack) {
                        graphics.fill3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset + packets[i].packet_pos, packet_width, packet_height, true);
                    } else {
                        graphics.fill3DRect(horizontal_offset + (packet_width + 7) * i, vertical_offset + vertical_clearance - packets[i].packet_pos, packet_width, packet_height, true);
                    }
                }
            }
        }  //End of Dynamic drawing

   // Start of Static drawing
        graphics.setColor(Color.black);
        int newvOffset = vertical_offset + vertical_clearance + packet_height;
        int newHOffset = horizontal_offset;

        graphics.drawString(message, newHOffset, newvOffset + 25);

        graphics.drawString("Packet", newHOffset + 15, newvOffset + 60);
        graphics.drawString("Acknowledge", newHOffset + 75, newvOffset + 60);
        graphics.drawString("Received Packet",newHOffset + 170, newvOffset + 60);
        graphics.drawString("Received acknowledge", newHOffset + 290, newvOffset + 60);


        //Lebels for showing
        graphics.setFont(temp_font);//BOLD Font
        graphics.drawString("Base = " + base, horizontal_offset + (packet_width + 7) * max_packets + 10, vertical_offset + vertical_clearance / 2);
        graphics.drawString("NextPacket = " + next_packet, horizontal_offset + (packet_width + 7) * max_packets + 10, vertical_offset + vertical_clearance / 2 + 20);

        graphics.setColor(Color.blue);

        //Labels for lines
        graphics.drawString("Receiver", horizontal_offset + (packet_width + 7) * max_packets + 10, vertical_offset + 12);
        graphics.drawString("Sender", horizontal_offset + (packet_width + 7) * max_packets + 10, vertical_offset + vertical_clearance + 12);
        graphics.setFont(base_font);//Reset Font

        //Box for Base / next numbers
        graphics.setColor(Color.black);
        graphics.draw3DRect(horizontal_offset + (packet_width + 7) * max_packets + 5, vertical_offset + vertical_clearance / 2 - 15, 100, 40, true);

        //Packet traveling to destination
        graphics.setColor(roam_pack_color); //red
        graphics.fill3DRect(newHOffset, newvOffset + 50, 10, 10, true);

        //Acknowledge packet traveling back to sender
        graphics.setColor(roam_ack_color); //orange
        graphics.fill3DRect(newHOffset + 60, newvOffset + 50, 10, 10, true);

        //Destination color
        graphics.setColor(destination_color); //blue
        graphics.fill3DRect(newHOffset + 155, newvOffset + 50, 10, 10, true);

        //Acknowledge packet received by sender
        graphics.setColor(ack_returned); //Green
        graphics.fill3DRect(newHOffset + 275, newvOffset + 50, 10, 10, true);

        g.drawImage(image, 0, 0, this);
    }    // method paint ends 

    /**********
     *Function for checking if any packets are inTransit
     *Controls state of main Thread
    /**********/
    public boolean inTransit(Packet pac[]) {
        for (int i = 0; i < pac.length; i++) {
            if (pac[i] == null) {
                return false;
            } else if (pac[i].on_way) {
                return true;
            }
        }
        return false;
    }

    /**********
     * Function for checking if receiver has
     * received all preceding packets
    ***********/
    public boolean check_upto_n(int packno) {
        for (int i = 0; i <= packno; i++) {
            if (!packets[i].reached_dest) {
                return false;
            }
        }
        return true;
    }

    /**********
     * Function used to reset the state of the applet
     * Clears all packets and stops all threads
    ************/
    public void reset() {
        for (int i = 0; i < max_packets; i++) {
            if (packets[i] != null) {
                packets[i] = null;
            }
        }
        base = 0;
        next_packet = 0;
        speed = 5;
        timerFlag = false;
        Sleep_flag = false;
        gbnThread = null;
        timerThread = null;
        //Reenable slowdown and speed up button for case of on pause
        if (stop_btn.getActionCommand() == "start")
        {
            slow_down_btn.setEnabled(true);
            speed_up_btn.setEnabled(true);
        }

        send_btn.setEnabled(true);

        stop_btn.setLabel("Stop");
        stop_btn.setActionCommand("stop");
        message = "Press 'Send New' to start.";
        repaint();
    }

    /**
     * Class to hold mostly Booleans for packets
     * Has a single integer for position tracking
     */
    class Packet
    {
        boolean on_way, reached_dest, acknowledged, packet_ack;
        int packet_pos;

        Packet()
        {
            on_way = false;
            reached_dest = false;
            acknowledged = false;
            packet_ack = true;
            packet_pos = 0;
        }

        Packet(boolean onway, int packetpos)
        {
            on_way = onway;
            reached_dest = false;
            acknowledged = false;
            packet_ack = true;
            packet_pos = packetpos;
        }

    }
}
