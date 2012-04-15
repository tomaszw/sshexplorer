package org.idempotentimplements.sshexplorer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import android.os.Parcel;
import android.os.Parcelable;

public class Login implements Parcelable {
	public String host="", user="", pass="", path="";
	public Login() {
	}
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel d, int flags) {
		d.writeString(host);
		d.writeString(user);
        d.writeString(path);
		d.writeString(pass);
		// TODO Auto-generated method stub
	}
	
	public void writeToFile(OutputStream out) throws IOException {
	    BufferedWriter w = new BufferedWriter(new PrintWriter(out));
	    w.write(host);
	    w.write('\n');
	    w.write(user);
        w.write('\n');
	    w.write(pass);
        w.write('\n');
	    w.write(path);
        w.write('\n');
	    w.flush();
	}

	public void readFromFile(InputStream in) throws IOException {
	    BufferedReader r = new BufferedReader(new InputStreamReader(in));
	    host = r.readLine();
	    user = r.readLine();
	    pass = r.readLine();
	    path = r.readLine();
	}
	
	public static Creator<Login> CREATOR = new Creator<Login>() {

		@Override
		public Login createFromParcel(Parcel source) {
			Login l = new Login();
			l.host = source.readString();
			l.user = source.readString();
            l.path = source.readString();
			l.pass = source.readString();
			return l;
		}

		@Override
		public Login[] newArray(int size) {
			return new Login[size];
		}
	};
}
