// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.android;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.debug.core.IEvaluatableBreakpoint;
import com.microsoft.java.debug.core.adapter.ICompletionsProvider;
import com.microsoft.java.debug.core.adapter.IEvaluationProvider;
import com.microsoft.java.debug.core.adapter.IHotCodeReplaceProvider;
import com.microsoft.java.debug.core.adapter.ISourceLookUpProvider;
import com.microsoft.java.debug.core.adapter.IVirtualMachineManagerProvider;
import com.microsoft.java.debug.core.adapter.ProviderContext;
import com.microsoft.java.debug.core.adapter.ProtocolServer;
import com.microsoft.java.debug.core.adapter.HotCodeReplaceEvent;
import com.microsoft.java.debug.core.protocol.Messages;
import com.microsoft.java.debug.core.protocol.Types.CompletionItem;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import io.reactivex.Observable;

/**
 * Provides one DAP connection for an Android JDWP process.
 */
public final class AndroidDebugServer {
  private final ServerSocket listener;
  private final int jdwpPort;
  private final GradleSourceSet sourceSet;
  private final String serial;
  private final AndroidExecutionService android;

  /** Create a DAP relay for a forwarded JDWP port. */
  public AndroidDebugServer(int jdwpPort, GradleSourceSet sourceSet, String serial,
      AndroidExecutionService android) throws IOException {
    this.listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    this.listener.setSoTimeout(90000);
    this.jdwpPort = jdwpPort;
    this.sourceSet = sourceSet;
    this.serial = serial;
    this.android = android;
  }

  /** Start accepting one DAP connection and return its address. */
  public String start() {
    Thread thread = new Thread(this::serve, "gradle-bsp-android-dap");
    thread.setDaemon(true);
    thread.start();
    return "tcp://127.0.0.1:" + listener.getLocalPort();
  }

  private void serve() {
    try (ServerSocket server = listener; Socket client = server.accept()) {
      ProviderContext providers = new ProviderContext();
      providers.registerProvider(IVirtualMachineManagerProvider.class,
          (IVirtualMachineManagerProvider) Bootstrap::virtualMachineManager);
      providers.registerProvider(ISourceLookUpProvider.class, new AndroidSourceLookup(sourceSet));
      providers.registerProvider(IEvaluationProvider.class, new NoEvaluation());
      providers.registerProvider(IHotCodeReplaceProvider.class, new NoHotCodeReplace());
      providers.registerProvider(ICompletionsProvider.class,
          (ICompletionsProvider) (StackFrame frame, String snippet, int line, int column) ->
              List.<CompletionItem>of());
      new AttachProtocol(client, providers, jdwpPort).run();
    } catch (IOException ignored) {
      // The client may cancel before it opens the connection.
    } finally {
      android.removeForward(serial, jdwpPort);
    }
  }

  private static final class AttachProtocol extends ProtocolServer {
    private final int jdwpPort;
    private final Set<Integer> launches = ConcurrentHashMap.newKeySet();

    AttachProtocol(Socket socket, ProviderContext providers, int jdwpPort) throws IOException {
      super(socket.getInputStream(), socket.getOutputStream(), providers);
      this.jdwpPort = jdwpPort;
    }

    @Override
    protected void dispatchRequest(Messages.Request request) {
      if ("launch".equals(request.command) || "attach".equals(request.command)) {
        if ("launch".equals(request.command)) {
          launches.add(request.seq);
        }
        request.command = "attach";
        request.arguments = new Gson().toJsonTree(Map.of(
            "hostName", "127.0.0.1", "port", jdwpPort, "timeout", 30000)).getAsJsonObject();
      }
      super.dispatchRequest(request);
    }

    @Override
    public void sendResponse(Messages.Response response) {
      if (launches.remove(response.request_seq)) {
        response.command = "launch";
      }
      if ("initialize".equals(response.command) && response.success && response.body != null) {
        JsonObject body = new Gson().toJsonTree(response.body).getAsJsonObject();
        body.addProperty("supportsConditionalBreakpoints", false);
        body.addProperty("supportsEvaluateForHovers", false);
        body.addProperty("supportsCompletionsRequest", false);
        body.addProperty("supportsLogPoints", false);
        body.addProperty("supportsSetVariable", false);
        response.body = body;
      }
      super.sendResponse(response);
    }
  }

  private static final class NoEvaluation implements IEvaluationProvider {
    @Override
    public boolean isInEvaluation(ThreadReference thread) {
      return false;
    }

    @Override
    public CompletableFuture<Value> evaluate(String expression, ThreadReference thread, int depth) {
      return unsupported();
    }

    @Override
    public CompletableFuture<Value> evaluate(String expression, ObjectReference context,
        ThreadReference thread) {
      return unsupported();
    }

    @Override
    public CompletableFuture<Value> evaluateForBreakpoint(IEvaluatableBreakpoint breakpoint,
        ThreadReference thread) {
      return unsupported();
    }

    @Override
    public CompletableFuture<Value> invokeMethod(ObjectReference context, String name,
        String signature, Value[] args, ThreadReference thread, boolean invokeSuper) {
      return unsupported();
    }

    @Override
    public void clearState(ThreadReference thread) {
    }

    private CompletableFuture<Value> unsupported() {
      return CompletableFuture.failedFuture(new UnsupportedOperationException(
          "Expression evaluation is not available for Android JDWP"));
    }
  }

  private static final class NoHotCodeReplace implements IHotCodeReplaceProvider {
    @Override
    public void onClassRedefined(Consumer<List<String>> consumer) {
    }

    @Override
    public CompletableFuture<List<String>> redefineClasses() {
      return CompletableFuture.failedFuture(new UnsupportedOperationException(
          "Hot code replace is not available for Android JDWP"));
    }

    @Override
    public Observable<HotCodeReplaceEvent> getEventHub() {
      return Observable.never();
    }
  }
}
