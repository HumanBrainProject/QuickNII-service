import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.InflaterInputStream;

public class Serv {
	
	/*
	 * Java, bigendian
	 * v0 (2 bytes)
	 * (s)len name (short-prefixed modified UTF-8, DataInput/Output)
	 * (i)len about (integer-prefixed deflated text)
	 * (s)xdim (s)ydim (s)zdim volume dimensions
	 * (b)views
	 * (b)type (s)len name (b)volidx {}|{(d)min (d)max}|palette
	 * (b)trfs
	 * (b)type (s)len name (d)4x3
	 * (b)vols
	 * (b)type defl...
	 *
	 * viewtype
	 * 0-x
	 * 1-graybytes
	 * 2-graybytepairs (2 vols)
	 * 3-indexbytes
	 * 4-indexbytepairs (2 vols)
	 * 5-RGB (3 vols)
	 * 
	 * trftype
	 * 0-x
	 * 1-matrix
	 * 
	 * voltype
	 * 0-x
	 * 1-deflate
	 */
	
    public static void main(String[] args) throws Exception {
        ServerSocket catcher = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
//        catcher.setSoTimeout(10000);
        System.out.println("Catcher: " + catcher.getLocalPort());
        final long start = System.currentTimeMillis();

        final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])));

        if (dis.readByte() != 'v' || dis.readByte() != '0')
            throw new Exception("v0?");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        final String NAME = dis.readUTF();
        final int abtlen = dis.readInt();
        final byte ABOUT[] = new byte[abtlen];
        dis.readFully(ABOUT);
        final short XDIM = dis.readShort();
        final short YDIM = dis.readShort();
        final short ZDIM = dis.readShort();

        final View VIEWS[] = new View[dis.readByte()];
        for (int i = 0; i < VIEWS.length; i++)
            VIEWS[i] = new View(dis);

        dos.writeUTF(NAME);
        dos.writeInt(abtlen);
        dos.write(ABOUT);
        dos.writeShort(XDIM);
        dos.writeShort(YDIM);
        dos.writeShort(ZDIM);
        dos.writeByte(VIEWS.length);
        for (int i = 0; i < VIEWS.length; i++)
            VIEWS[i].config(dos);

        final byte trfs = dis.readByte();
        dos.writeByte(trfs);
        for (int i = 0; i < trfs; i++) {
            dos.writeUTF(dis.readUTF());
            for (int j = 0; j < 12; j++)
                dos.writeDouble(dis.readDouble());
        }
        dos.close();

        final byte VOLS[][] = new byte[dis.readByte()][];
        for (int i = 0; i < VOLS.length; i++) {
            VOLS[i] = new byte[XDIM * YDIM * ZDIM];
            final byte buf[] = new byte[dis.readInt()];
            System.out.println("" + i + " " + buf.length);
            dis.readFully(buf);
            new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(buf))).readFully(VOLS[i]);
        }

        final SliceBase xthread = new XSlice(XDIM, YDIM, ZDIM, VIEWS, VOLS);
        final SliceBase ythread = new YSlice(XDIM, YDIM, ZDIM, VIEWS, VOLS);
        final SliceBase zthread = new ZSlice(XDIM, YDIM, ZDIM, VIEWS, VOLS);
        final SliceBase fthread = new FreeSlice(XDIM, YDIM, ZDIM, VIEWS, VOLS);
        System.out.println("XPort: " + xthread.getLocalPort());
        System.out.println("YPort: " + ythread.getLocalPort());
        System.out.println("ZPort: " + zthread.getLocalPort());
        System.out.println("FPort: " + fthread.getLocalPort());

        System.out.println("" + (System.currentTimeMillis() - start) + ": Server ready.");
        System.out.flush();
        
///////////////////////////////////////////////////////////////////////////////////

        if(args.length==1)        
            Runtime.getRuntime().exec(String.format("wine QuickNII.exe %s %d %d %d %d %d", args[0], catcher.getLocalPort(), xthread.getLocalPort(), ythread.getLocalPort(), zthread.getLocalPort(), fthread.getLocalPort()));
        else
            Runtime.getRuntime().exec(String.format("QuickNII.exe %s %d %d %d %d %d", args[0], catcher.getLocalPort(), xthread.getLocalPort(), ythread.getLocalPort(), zthread.getLocalPort(), fthread.getLocalPort()));
        
///////////////////////////////////////////////////////////////////////////////////
        
        final Socket s = catcher.accept();
        dos = new DataOutputStream(s.getOutputStream());
        dos.writeInt(baos.size());
        baos.writeTo(dos);
        dos.flush();
    }
}
