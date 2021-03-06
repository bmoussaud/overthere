package com.xebialabs.overthere.ssh;

import static com.xebialabs.overthere.ConnectionOptions.ADDRESS;
import static com.xebialabs.overthere.ConnectionOptions.OPERATING_SYSTEM;
import static com.xebialabs.overthere.ConnectionOptions.PASSWORD;
import static com.xebialabs.overthere.ConnectionOptions.USERNAME;
import static com.xebialabs.overthere.OperatingSystemFamily.UNIX;
import static com.xebialabs.overthere.ssh.SshConnectionBuilder.CONNECTION_TYPE;
import static com.xebialabs.overthere.ssh.SshConnectionBuilder.SSH_PROTOCOL;
import static com.xebialabs.overthere.ssh.SshConnectionBuilder.SUDO_COMMAND_PREFIX;
import static com.xebialabs.overthere.ssh.SshConnectionBuilder.SUDO_QUOTE_COMMAND;
import static com.xebialabs.overthere.ssh.SshConnectionBuilder.SUDO_USERNAME;
import static com.xebialabs.overthere.ssh.SshConnectionType.SUDO;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.xebialabs.overthere.CmdLine;
import com.xebialabs.overthere.CmdLineArgument;
import com.xebialabs.overthere.ConnectionOptions;

public class SshSudoConnectionSudoCommandTest {

	private ConnectionOptions connectionOptions;

	@Before
	public void init() {
		connectionOptions = new ConnectionOptions();
		connectionOptions.set(CONNECTION_TYPE, SUDO);
		connectionOptions.set(OPERATING_SYSTEM, UNIX);
		connectionOptions.set(ADDRESS, "nowhere.example.com");
		connectionOptions.set(USERNAME, "some-user");
		connectionOptions.set(PASSWORD, "foo");
		connectionOptions.set(SUDO_USERNAME, "some-other-user");
	}

	@Test
	public void shouldUseDefaultSudoCommandPrefixIfNotConfiguredExplicitly() {
		SshSudoConnection connection = new SshSudoConnection(SSH_PROTOCOL, connectionOptions);
		
		List<CmdLineArgument> args = connection.processCommandLine(CmdLine.build("ls", "/tmp")).getArguments();
		assertThat(args.size(), equalTo(5));
		assertThat(args.get(0).toString(), equalTo("sudo"));
		assertThat(args.get(1).toString(), equalTo("-u"));
		assertThat(args.get(2).toString(), equalTo("some-other-user"));
		assertThat(args.get(3).toString(), equalTo("ls"));
		assertThat(args.get(4).toString(), equalTo("/tmp"));
	}

	@Test
	public void shouldUseConfiguredSudoCommandPrefix() {
		connectionOptions.set(SUDO_COMMAND_PREFIX, "sx -u {0}");
		SshSudoConnection connection = new SshSudoConnection(SSH_PROTOCOL, connectionOptions);
		
		List<CmdLineArgument> args = connection.processCommandLine(CmdLine.build("ls", "/tmp")).getArguments();
		assertThat(args.size(), equalTo(5));
		assertThat(args.get(0).toString(), equalTo("sx"));
		assertThat(args.get(1).toString(), equalTo("-u"));
		assertThat(args.get(2).toString(), equalTo("some-other-user"));
		assertThat(args.get(3).toString(), equalTo("ls"));
		assertThat(args.get(4).toString(), equalTo("/tmp"));
	}

	@Test
	public void shouldUseConfiguredSudoCommandPrefixWithoutCurlyZero() {
		connectionOptions.set(SUDO_COMMAND_PREFIX, "sx");
		SshSudoConnection connection = new SshSudoConnection(SSH_PROTOCOL, connectionOptions);

		List<CmdLineArgument> args = connection.processCommandLine(CmdLine.build("ls", "/tmp")).getArguments();
		assertThat(args.size(), equalTo(3));
		assertThat(args.get(0).toString(), equalTo("sx"));
		assertThat(args.get(1).toString(), equalTo("ls"));
		assertThat(args.get(2).toString(), equalTo("/tmp"));
	}

	@Test
	public void shouldQuoteOriginalCommand() {
		connectionOptions.set(SUDO_COMMAND_PREFIX, "su -u {0}");
		connectionOptions.set(SUDO_QUOTE_COMMAND, true);
		SshSudoConnection connection = new SshSudoConnection(SSH_PROTOCOL, connectionOptions);
		
		CmdLine cmdLine = connection.processCommandLine(CmdLine.build("ls", "/tmp"));
		List<CmdLineArgument> args = cmdLine.getArguments();
		assertThat(args.size(), equalTo(4));
		assertThat(args.get(0).toString(), equalTo("su"));
		assertThat(args.get(1).toString(), equalTo("-u"));
		assertThat(args.get(2).toString(), equalTo("some-other-user"));
		assertThat(args.get(3).toString(), equalTo("ls\\ /tmp"));
		assertThat(cmdLine.toString(), equalTo("su -u some-other-user ls\\ /tmp"));
	}

	@Test
	public void commandWithPipeShouldHaveTwoSudoSectionsIfNotQuotingCommand() {
		SshSudoConnection connection = new SshSudoConnection(SSH_PROTOCOL, connectionOptions);

		List<CmdLineArgument> prepended = connection.prefixWithSudoCommand(CmdLine.build("a", "|", "b")).getArguments();
		assertThat(prepended.size(), equalTo(9));
		assertThat(prepended.get(0).toString(), equalTo("sudo"));
		assertThat(prepended.get(5).toString(), equalTo("sudo"));
	}

	@Test
	public void commandWithPipeShouldNotHaveTwoSudoSectionsIfQuotingCommand() {
		connectionOptions.set(SUDO_COMMAND_PREFIX, "su -u {0}");
		connectionOptions.set(SUDO_QUOTE_COMMAND, true);
		SshSudoConnection connection = new SshSudoConnection(SSH_PROTOCOL, connectionOptions);

		List<CmdLineArgument> prepended = connection.prefixWithSudoCommand(CmdLine.build("a", "|", "b")).getArguments();
		assertThat(prepended.size(), equalTo(4));
		assertThat(prepended.get(0).toString(), equalTo("su"));
		assertThat(prepended.get(1).toString(), equalTo("-u"));
		assertThat(prepended.get(2).toString(), equalTo("some-other-user"));
		assertThat(prepended.get(3).toString(), equalTo("a\\ |\\ b"));
	}

	@Test
	public void commandWithSemiColonShouldHaveTwoSudoSectionsIfNotQuotingCommand() {
		SshSudoConnection connection = new SshSudoConnection(SSH_PROTOCOL, connectionOptions);

		List<CmdLineArgument> prepended = connection.prefixWithSudoCommand(CmdLine.build("a", ";", "b")).getArguments();
		assertThat(prepended.size(), equalTo(9));
		assertThat(prepended.get(0).toString(), equalTo("sudo"));
		assertThat(prepended.get(5).toString(), equalTo("sudo"));
	}

	@Test
	public void commandWithSemiColonShouldNotHaveTwoSudoSectionsIfQuotingCommand() {
		connectionOptions.set(SUDO_COMMAND_PREFIX, "su -u {0}");
		connectionOptions.set(SUDO_QUOTE_COMMAND, true);
		SshSudoConnection connection = new SshSudoConnection(SSH_PROTOCOL, connectionOptions);

		List<CmdLineArgument> prepended = connection.prefixWithSudoCommand(CmdLine.build("a", ";", "b")).getArguments();
		assertThat(prepended.size(), equalTo(4));
		assertThat(prepended.get(0).toString(), equalTo("su"));
		assertThat(prepended.get(1).toString(), equalTo("-u"));
		assertThat(prepended.get(2).toString(), equalTo("some-other-user"));
		assertThat(prepended.get(3).toString(), equalTo("a\\ \\\\\\;\\ b"));
	}

}
