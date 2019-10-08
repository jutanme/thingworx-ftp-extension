package ftp_extension;

import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.things.Thing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;

@ThingworxConfigurationTableDefinitions(tables = { @com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinition(name = "ConnectionInfo", description = "FTP Server Connection Parameters", isMultiRow = false, ordinal = 0, dataShape = @com.thingworx.metadata.annotations.ThingworxDataShapeDefinition(fields = {
		@com.thingworx.metadata.annotations.ThingworxFieldDefinition(ordinal = 0, name = "FTPServer", description = "FTP server name", baseType = "STRING", aspects = {
				"defaultValue:ftp.domain.com", "friendlyName:FTP Server Address" }),
		@com.thingworx.metadata.annotations.ThingworxFieldDefinition(ordinal = 1, name = "FTPPort", description = "FTP server port", baseType = "INTEGER", aspects = {
				"defaultValue:21", "friendlyName:FTP Server Port" }),
		@com.thingworx.metadata.annotations.ThingworxFieldDefinition(ordinal = 2, name = "FTPUser", description = "FTP username", baseType = "STRING", aspects = {
				"defaultValue:", "friendlyName:FTP Username" }),
		@com.thingworx.metadata.annotations.ThingworxFieldDefinition(ordinal = 3, name = "FTPPass", description = "FTP password", baseType = "PASSWORD", aspects = {
				"defaultValue:", "friendlyName:FTP Password" }),
				@com.thingworx.metadata.annotations.ThingworxFieldDefinition(ordinal = 4, name = "UseSSL", description = "Use TLS/SSL for connection", baseType = "BOOLEAN", aspects = {
						"defaultValue:false", "friendlyName:Use TLS/SSL" })

})) })
public class FTP_Server extends Thing {

	
	private static final long serialVersionUID = 6267816907697604977L;
		
	protected static Logger _logger = LogUtilities.getInstance()
			.getApplicationLogger(FTP_Server.class);
	private String FTPServer = "";
	private Integer FTPPort = 21;
	private String FTPUser = "";
	private String FTPPass="";
	private boolean bool_UseSSL=false;
	FTP_Utils FTPUtilitiesClass;
	//private  FileRepositoryThing fs;

	protected void initializeThing() throws Exception {
		//getConfigurationData().getValue("Table", "propName");
		//getConfigurationTable("dasdA").getField("dasda").toValueCollection().getValue("asdada");
		this.FTPServer = ((String) getConfigurationData().getValue(
				"ConnectionInfo", "FTPServer"));
		this.FTPPort = ((Integer) getConfigurationData().getValue(
				"ConnectionInfo", "FTPPort"));
		this.FTPUser=((String) getConfigurationData().getValue(
				"ConnectionInfo", "FTPUser"));
		this.FTPPass=((String) getConfigurationData().getValue(
				"ConnectionInfo", "FTPPass"));
		this.bool_UseSSL=((boolean) getConfigurationData().getValue(
				"ConnectionInfo", "UseSSL"));
		
		FTP_Utils.DestroyInstance();
		FTPUtilitiesClass=FTP_Utils.getInstance(FTPServer, FTPPort.intValue(), FTPUser, FTPPass,bool_UseSSL);
		
	}

	@ThingworxServiceDefinition(name = "GetFileList", description = "Return file list from FTP server")
	@ThingworxServiceResult(name = "Result", description = "Result", baseType = "INFOTABLE",aspects={"dataShape:FTPServerListingShape"})
	
	public InfoTable GetFileList(@ThingworxServiceParameter(name="FTPPath", description="Name of FTP server path for listing content.", baseType="STRING", aspects={"defaultValue:"}) String FTPPath)  {
		InfoTable result=null;
		try {
			 result = InfoTableInstanceFactory
					.createInfoTableFromDataShape("FTPServerListingShape");
			return FTPUtilitiesClass.GetFileList(FTPPath);
		} catch (Throwable e) {
		

			if (FTPUtilitiesClass.client != null && FTPUtilitiesClass.client.isConnected()) {
				try {
					FTPUtilitiesClass.client.disconnect(true);
				} catch (IllegalStateException | IOException
						| FTPIllegalReplyException | FTPException e1) {
					
					e1.printStackTrace();
				}
			}
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			_logger.error(errors.toString());
	
			
			ValueCollection vc = new ValueCollection();
			vc.put("Name", new StringPrimitive(errors.toString()));
			vc.put("Path", new StringPrimitive(""));
			vc.put("Type", new StringPrimitive("ERROR"));
			vc.put("Size", new NumberPrimitive(0));
			result.addRow(vc);
			return result;
			

		}
		
		
		
	}
	
	@ThingworxServiceDefinition(name = "DownloadFile", description = "Download a FTP server file to a repository")
	@ThingworxServiceResult(name = "Result", description = "Result", baseType = "STRING")
	public String DownloadFile(@ThingworxServiceParameter(name="FTPFileName", description="Name of FTP File", baseType="STRING", aspects={"defaultValue:"}) String FTPFile,
			@ThingworxServiceParameter(name="FTPFilePath", description="Source file path from the FTP server", baseType="STRING", aspects={"defaultValue:"}) String FTPPath,
			@ThingworxServiceParameter(name="FileRepository", description="File repository", baseType="THINGNAME")  String FileRepository
			)  {
		
		
		try {
			return FTPUtilitiesClass.DownloadFile(FTPFile, FTPPath, FileRepository);
		} catch (Throwable e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			_logger.error(errors.toString());
			return errors.toString();
		}
	}
	
	@ThingworxServiceDefinition(name = "UploadFile", description = "Upload a file from a repository to the FTP Server")
	@ThingworxServiceResult(name = "Result", description = "Result", baseType = "STRING")
	public String UploadFile(@ThingworxServiceParameter(name="RepoFileName", description="Name of Repository File", baseType="STRING", aspects={"defaultValue:"}) String RepoFile,
			@ThingworxServiceParameter(name="RepoFilePath", description="Repo file path", baseType="STRING", aspects={"defaultValue:"}) String RepoPath,
			@ThingworxServiceParameter(name="FTPFileName", description="Name of FTP File", baseType="STRING", aspects={"defaultValue:"}) String FTPFile,
			@ThingworxServiceParameter(name="FTPFilePath", description="FTP file path", baseType="STRING", aspects={"defaultValue:"}) String FTPPath,
			@ThingworxServiceParameter(name="FileRepository", description="File repository", baseType="THINGNAME")  String FileRepository
			)  {
	
		try {
			return FTPUtilitiesClass.UploadFile(FileRepository, RepoFile, RepoPath, FTPFile, FTPPath );
		} catch (Throwable e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			_logger.error(errors.toString());
			return errors.toString();
		}
	}

	@ThingworxServiceDefinition(name = "MyService", description = "", category = "", isAllowOverride = false, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "Result", description = "", baseType = "TEXT", aspects = {})
	public String MyService(
			@ThingworxServiceParameter(name = "MyParam", description = "", baseType = "STRING", aspects = {
					"isRequired:true" }) String MyParam) {
		_logger.trace("Entering Service: MyService");
		_logger.trace("Exiting Service: MyService");
		return null;
	}
	
	

}
