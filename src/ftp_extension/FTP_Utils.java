package ftp_extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.metadata.FieldDefinition;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class FTP_Utils {
	public InfoTable result;

	public FTPClient client;
	private String str_Server;
	private Number int_Port;
	private String str_Username;
	private String str_Password;
	private boolean bool_UseSSL;
	
	
	private static FTP_Utils instance = null;
	private FTP_Utils() {
	      
	}
	
	public static FTP_Utils getInstance(String str_Server, Number int_Port, String str_Username,
			String str_Password,boolean bool_UseSSL) throws Exception {
	      if(instance == null) {
	         instance = new FTP_Utils(str_Server, int_Port, str_Username,str_Password,bool_UseSSL);
	      }
	      return instance;
	}
	public static void DestroyInstance()
	
	{
		instance=null;
	}
	
	private FTP_Utils(String str_Server, Number int_Port, String str_Username,
			String str_Password,boolean bool_UseSSL) throws Exception {
		super();
		this.str_Server = str_Server;
		this.int_Port = int_Port;
		this.str_Username = str_Username;
		this.str_Password = str_Password;
		this.bool_UseSSL = bool_UseSSL;
		result = InfoTableInstanceFactory
				.createInfoTableFromDataShape("FTPServerListingShape");
	
	}

	private void Connect() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		client = new FTPClient();
		
		if (bool_UseSSL)
		{
		TrustManager[] trustManager = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustManager, new SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
		SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		client.setSSLSocketFactory(sslSocketFactory);
		
		client.setSecurity(FTPClient.SECURITY_FTPS);
		}
	
		client.connect(str_Server, int_Port.intValue());
		
		client.login(str_Username, str_Password);
	}

	private void Disconnect() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		client.disconnect(true);
	}

	public InfoTable GetFileList(String path) throws Throwable {

			FieldDefinition fs = new FieldDefinition();
			fs.setOrdinal(0);
			
			
			result.removeAllRows();
			Connect();
			FTPFile[] files=null;
			if (path==null)
			{
			 files = client.list();
			}
			else
			{
				client.changeDirectory(path);
				files = client.list();
			}
			int int_Files_Length = files.length;
			if (int_Files_Length > 0) {
				for (int j = 0; j < int_Files_Length; j++) {
					String str_Current_Directory=client.currentDirectory();
					if (!str_Current_Directory.equals("/")) str_Current_Directory=str_Current_Directory+"/";
					if (files[j].getType() == FTPFile.TYPE_FILE) {
						ValueCollection vc = new ValueCollection();
						vc.put("Name",
								new StringPrimitive(files[j].getName()));
						vc.put("Path",
								new StringPrimitive(str_Current_Directory));
						vc.put("Type", new StringPrimitive("FILE"));
						vc.put("Size", new NumberPrimitive(files[j].getSize()));
						result.addRow(vc);
					}
					if (files[j].getType() == FTPFile.TYPE_DIRECTORY) {
						// client.changeDirectory(files[j].getName());
						// GetFilesRecursive();
						ValueCollection vc = new ValueCollection();
						vc.put("Name",
								new StringPrimitive(files[j].getName()));
						vc.put("Path",
								new StringPrimitive(str_Current_Directory));
						vc.put("Type", new StringPrimitive("FOLDER"));
						vc.put("Size", new NumberPrimitive(0));
						result.addRow(vc);

					}
				}
			}

			Disconnect();
			return result;
		
	}

	// Not used at the moment because, depending on FTP folder depth listing
	// might take a lot of time,
	// the "No Transfer Timeout" will kick in and the server will disconnect you
	// with Code 421.
	// The Timeouts for FTP server are:
	// 1. Login timeout - this one is for connections that never supply login
	// data.
	// 2. Connections timeout - If no activity is detected within that time the
	// connection is terminated. Any action (including NOOP) will reset this
	// counter.
	// 3. No Transfer timeout - Only an actual transfer will reset that one.
	// Listing folder content is not regarded as transfer.
	@SuppressWarnings("unused")
	private void GetFilesRecursive() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException, FTPDataTransferException,
			FTPAbortedException, FTPListParseException {
		FTPFile[] files = client.list();
		int int_Files_Length = files.length;
		
		for (int j = 0; j < int_Files_Length; j++) {
			if (files[j].getType() == FTPFile.TYPE_FILE) {
				ValueCollection vc = new ValueCollection();
				vc.put("FileName", new StringPrimitive(files[j].getName()));
				vc.put("Path", new StringPrimitive(client.currentDirectory()));
				result.addRow(vc);
			}
			if (files[j].getType() == FTPFile.TYPE_DIRECTORY) {
				client.changeDirectory(files[j].getName());
				GetFilesRecursive();
				client.changeDirectoryUp();
				return;
			}
		}
	}

	public String DownloadFile(String fTPFile, String fTPPath, String fileRepository) throws Throwable {

		
			Connect();
			
			Thing thing = ThingUtilities.findThing(fileRepository);
			FileRepositoryThing fileRepoThing = (FileRepositoryThing)thing;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			client.download(fTPPath+fTPFile, bos, 0, null);
			fileRepoThing.CreateBinaryFile(fTPFile, bos.toByteArray(),true);
			
			Disconnect();
			
			return "File "+fTPFile +" was transferred successfully to repository"+fileRepository+".";
	}
	
	public String UploadFile(String fileRepository, String RepoFile, String RepoPath, String str_Destination_Name,String str_Destination_Path) throws Throwable {

			Connect();
			
			Thing thing = ThingUtilities.findThing(fileRepository);
			FileRepositoryThing fileRepoThing = (FileRepositoryThing)thing;
			
			ByteArrayInputStream bis = new ByteArrayInputStream(fileRepoThing.LoadBinary(RepoPath+RepoFile));
			
			client.upload(str_Destination_Path+str_Destination_Name, bis, 0, 0, null);
			
			Disconnect();
			return "File "+str_Destination_Name +" was transferred successfully to FTP Server on directory "+str_Destination_Path+".";
		
	}

}
