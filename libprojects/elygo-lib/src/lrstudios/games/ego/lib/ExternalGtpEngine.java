package lrstudios.games.ego.lib;

import android.content.Context;
import android.util.Log;

import java.io.*;


/**
 * Provides functions to start a GTP engine in another process and communicate with it.
 */
public abstract class ExternalGtpEngine extends GtpEngine
{
    protected static Process _engineProcess;

    private static final String TAG = "ExternalGtpEngine";

    protected String[] _processArgs;
    protected Thread _stdErrThread;
    protected OutputStreamWriter _writer;
    protected BufferedReader _reader;


    /**
     * Gets (and creates if needed) the file where the engine is located.
     */
    protected abstract File getEngineFile();


    public ExternalGtpEngine(Context context, String[] processArgs)
    {
        super(context);
        _processArgs = processArgs;
    }


    @Override
    public boolean init()
    {
        try
        {
            if (_engineProcess == null)
            {
                int len = _processArgs.length;
                String[] args = new String[len + 1];
                args[0] = getEngineFile().getAbsolutePath();
                System.arraycopy(_processArgs, 0, args, 1, len);
                _engineProcess = new ProcessBuilder(args).start();
            }

            InputStream is = _engineProcess.getInputStream();
            _reader = new BufferedReader(new InputStreamReader(is), 8192);
            _writer = new OutputStreamWriter(_engineProcess.getOutputStream());

            if (_stdErrThread != null && _stdErrThread.isAlive())
                _stdErrThread.interrupt();
            _stdErrThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    InputStream is = _engineProcess.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
                    try
                    {
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            Log.e(TAG, "[Error] " + line);
                            if (Thread.currentThread().isInterrupted())
                                return;
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            _stdErrThread.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String sendGtpCommand(String command)
    {
        try
        {
            Log.v(TAG, "Send: " + command);
            _writer.write(command + "\n");
            _writer.flush();
            String line;
            char ch;
            do
            {
                line = _reader.readLine(); // TODO can return null
                Log.v(TAG, " >> " + line);
                ch = line.length() > 0 ? line.charAt(0) : 0;
            } while (ch != '=' && ch != '?');
            return line;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}