package com.tomaszw.sshexplorer;

import android.os.Parcel;
import android.os.Parcelable;

public class Login implements Parcelable {
	public String host, user, pass, path;
	public Login() {
		host = "192.168.1.100";
		user = "tomaszw";
		pass = "a1l2v3a4";
		path = "";
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
