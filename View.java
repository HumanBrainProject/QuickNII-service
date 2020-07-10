import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.zip.InflaterInputStream;

final class View {
    final String name;
    final byte type;
    final byte idx;

    final double min;
    final double max;
    final byte r[];
    final byte g[];
    final byte b[];
    final String names[];

    View(final DataInputStream dis) throws Exception {
        type = dis.readByte();
        name = dis.readUTF();
        idx = dis.readByte();
        switch (type) {
            case 1:
            case 2:
                min = dis.readDouble();
                max = dis.readDouble();
                r = g = b = null;
                names = null;
                break;
            case 3:
            case 4: {
                min = max = 0;
                final int l = dis.readInt();
                final byte bp[] = new byte[l];
                dis.readFully(bp);
                final DataInputStream disp = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(bp)));
                final short s = disp.readShort();
                r = new byte[s + 1];
                g = new byte[s + 1];
                b = new byte[s + 1];
                names = new String[s + 1];
                names[0] = "";
                for (int i = 1; i < s + 1; i++) {
                    r[i] = disp.readByte();
                    g[i] = disp.readByte();
                    b[i] = disp.readByte();
                    names[i] = disp.readUTF();
                }
                break;
            }
            case 5:
                min = max = 0;
                r = g = b = null;
                names = null;
                break;
            default:
                throw new Exception("ViewType=" + type);
        }
    }

    final void config(final DataOutputStream dos) throws Exception {
        dos.write(type);
        dos.writeUTF(name);
        switch (type) {
            case 1:
            case 2:
                dos.writeDouble(min);
                dos.writeDouble(max);
                break;
            case 3:
            case 4: {
                dos.writeShort(names.length);
                for (int i = 0; i < names.length; i++) {
                    dos.writeByte(0);
                    dos.writeByte(r[i]);
                    dos.writeByte(g[i]);
                    dos.writeByte(b[i]);
                    dos.writeUTF(names[i]);
                }
                break;
            }
            case 5:
                break;
            default:
                throw new Exception("ViewType=" + type);
        }
    }
}
