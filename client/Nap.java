import java.io.*;
import java.net.*;

//���ڼ���hashֵ
import org.apache.commons.codec.digest.DigestUtils;

class Global
{
	public static String path = "";
}

class peer_server implements Runnable
{
	// ʵ��һ���������ļ�������
	public void run()
	{
		try
		{
			// Ԥ����
			String path = Global.path;

			// �������ڽ��շ�������Ϣ��ServerSocket
			ServerSocket comServSock = new ServerSocket(7701);
			
			// �������ڽ�����һ��peer��Ϣ�������ļ���ServerSocket
			ServerSocket fileServSock = new ServerSocket(7702);
			
			while(true)
			{
				Socket socket = comServSock.accept();
				
				//�������������
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), false);
				
				String response = "";
				String[] respArray;

				// ѭ��������������ֱ�����յ�"HELLO"����"QUIT"������Ϣ
				while(!response.equals("HELLO") && !response.equals("QUIT"))
				{
					response = in.readLine();					
					// ������յ���������Ϣ��OPEN���ظ�ȷ����ϢHELLO
					if(response.equals("HELLO"))
					{
						out.println("ACCEPT");
						out.flush();
					}
				}

				// ѭ��������������ֱ���յ�QUIT������Ϣ
				while(!response.equals("QUIT"))
				{
					response = in.readLine();
					
					//��������ֶδ���
					respArray = response.split(" ");					

					// syntax: GET [filename]
					if(respArray[0].equals("GET"))
					{
						try
						{
							// ������ļ�����Ϊ��
							if(!respArray[1].isEmpty())
							{
								// �½�һ�������ļ������socket
								Socket fileSocket = fileServSock.accept();

								File peerfile = new File(path + File.separator + respArray[1]);								
								byte[] buffer = new byte[(int)peerfile.length()];
								BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(peerfile));
								fileIn.read(buffer, 0, buffer.length);
								BufferedOutputStream fileOut = new BufferedOutputStream(fileSocket.getOutputStream());
								fileOut.write(buffer, 0, buffer.length);
								fileOut.flush();
								fileIn.close();
								fileOut.close();
								fileSocket.close();
								
								out.println("OK");
								out.flush();
							}
						}
						catch (Exception e)
						{
							out.print("ERROR "+e);
							out.flush();
						}
					}
					else if(response.equals("CLOSE"))
					{
						continue;
					}
				}
				out.print("GOODBYE");
				out.flush();
				socket.close();
			}
		}
		catch (Exception e)
		{
			System.out.println("\033[1;31m[����] >>\033[0m "+e);			
			System.exit(-1);
		}
	}
}

public class Nap
{
	public static void error_handler(String err)
	{
		System.out.println("\033[1;31m[����] >>\033[0m " + err.substring(6));
		System.exit(-1);
	}

	// Main method
	public static void main(String[] args)
	{
		try
		{
			System.out.println("Nap�ͻ���");

			Socket socket;
			BufferedReader in;
			PrintWriter out;
			
			// ��ʼ�����ڽ����û������stdin 
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

			String server;//P2P������IP
			int port;//P2P�������˿�
			String path;//����P2P����Ŀ¼
			
			String request = "";
			String[] reqArray;
			String response;
			String[] respArray;
			
			//��ȡ������Ҫ��Ϣ
			System.out.print("��������IP��ַ >> ");
			server = stdin.readLine();
			System.out.print("�������Ķ˿ں� >> ");
			port = Integer.parseInt(stdin.readLine());
			System.out.print("�����Ĺ���Ŀ¼ >> ");
			path = stdin.readLine();
			Global.path = path;
			
			socket = new Socket(server, port);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));//���������ص���Ϣ
			out = new PrintWriter(socket.getOutputStream(), false);//���͸�����������Ϣ

			// ��ӡ���������ص���Ϣ
			System.out.println(in.readLine());

			// ����������Ϣ"CONNECT"����ʼ����
			out.print("CONNECT");
			out.flush();
			response = in.readLine();

			// ����ȷ����Ϣ
			if(!response.equals("ACCEPT"))
			{
				System.out.println("\033[1;31m[����] >>\033[0m �����˷��͵�������Ϣδ�ܽ��յ���ȷ��ȷ�ϰ�");
				System.exit(-1);	
			}
			else
			{
				System.out.println("\033[1;32m[�ɹ�] >>\033[0m �ɹ����ӵ�Napd������ " + server + ":" + port);
			}

			File folder = new File(path);
			File[] files = folder.listFiles();
			FileInputStream f_stream;
			String filename;
			String filehash;
			String filesize;
			System.out.println("[��Ϣ] ����Ϊ����Ŀ¼ " + path + " �����ļ�����...");
			int index_total = 0;
			
			for(int i = 0; i < files.length; i++)
			{
				if(files[i].isFile())
				{
					filename = files[i].getName();
					f_stream = new FileInputStream(files[i]);
					filehash = DigestUtils.md5Hex(f_stream);
					f_stream.close();
					filesize = String.valueOf(files[i].length());
					
					out.print("ADD " + filename + " " + filehash + " " + filesize);
					out.flush();
					response = in.readLine();
					
					if(!response.equals("OK"))
						error_handler(response);
					else
					{
						System.out.print(". ");
						index_total++;
					}
				}
			}

			System.out.println("\n\033[1;32m[�ɹ�] >>\033[0m �ɹ���� " + index_total + " ���ļ���Ϣ��������");

			// �����ļ��������߳�
			Runnable run = new peer_server();
			Thread thread = new Thread(run);
			thread.start();

			System.out.println("[��Ϣ] �ȴ��û�����");

			do
			{
				System.out.print(">> ");
				request = stdin.readLine();
				reqArray = request.split(" ");
				
				if(request.equals("list"))
				{
					System.out.println("[��Ϣ] ����������������ļ��б�...");

					// ����LIST����
					out.print("LIST");
					out.flush();

					int list_total = 0;
					response = in.readLine();
					respArray = response.split(" ");
					while((!respArray[0].equals("OK")) && (!respArray[0].equals("ERROR")))
					{
						list_total++;						
						System.out.println(String.format("[%2d] : %20s [�ļ���С: %10s]", new Object[] { new Integer(list_total), respArray[0], respArray[1] }));									
						
						response = in.readLine();
						respArray = response.split(" ");
					}
					System.out.println("[��Ϣ] һ����ȡ�� " + list_total + " ���ļ�");
					
					if(!response.equals("OK"))
						error_handler(response);
				}
				else if(reqArray[0].equals("request"))
				{
					try
					{
						if(!reqArray[1].isEmpty())
						{
							//����REQUEST
							out.print("REQUEST " + reqArray[1]);
							out.flush();
							
							response = in.readLine();
							respArray = response.split(" ");
							if(respArray[0].equals("OK"))
								System.out.println("\033[1;31m[����] >>\033[0m �ڷ������ϲ�δ�ҵ��ļ�'" + reqArray[1]);

							while((!respArray[0].equals("OK")) && (!respArray[0].equals("ERROR")))
							{
								//respArray��ʽ��peer��IP+�ļ���С
								Socket comSocket = new Socket(respArray[0], 7701);
								
								String comResponse;
								BufferedReader comIn = new BufferedReader(new InputStreamReader(comSocket.getInputStream()));
								PrintWriter comOut = new PrintWriter(comSocket.getOutputStream(), false);
								
								//��֤���
								comOut.println("HELLO");
								comOut.flush();
								comResponse = comIn.readLine();
								
								//ȷ��
								if(!comResponse.equals("ACCEPT"))
								{
									System.out.println("\033[1;31m[����] >>\033[0m �ͻ���������Ϣ��֤ʧ��");
									System.exit(-1);
								}
	
								Socket fileSocket = new Socket(respArray[0], 7702);
								comOut.println("GET " + reqArray[1]);
								comOut.flush();
								InputStream fileIn = fileSocket.getInputStream();
								
								File f = new File(path+File.separator+"recv");
								 if (!f.exists()) 
								 {
									 f.mkdirs();
								 }
								BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(path+File.separator+"recv"+File.separator + reqArray[1]));
								int bytesRead,current = 0;
								
								byte[] buffer = new byte[Integer.parseInt(respArray[1])];								
								bytesRead = fileIn.read(buffer, 0, buffer.length);
								current = bytesRead;

								System.out.println("[��Ϣ] ��ʼ�����ļ�...");
								do
								{
									System.out.print(". ");

									bytesRead = fileIn.read(buffer, current, (buffer.length - current));
									if(bytesRead >= 0)
										current += bytesRead;
								} while(bytesRead > -1 && buffer.length != current);

								fileOut.write(buffer, 0, current);
								fileOut.flush();

								System.out.println("\n\033[1;32m[�ɹ�] >>\033[0m �ļ�����ɹ�");
								
								fileIn.close();
								fileOut.close();
								fileSocket.close();
								
								respArray[0] = "OK";
                                
                                response = in.readLine();
                                respArray = response.split(" ");							
							}
                            
                            
							if(!respArray[0].equals("OK"))
								error_handler(response);
						}
					}
					catch (Exception e)
					{
						System.out.println("\033[1;31m[����] >>\033[0m "+e);
					}
				}
			} while(!request.equals("quit"));

			out.print("QUIT");
			out.flush();

			response = in.readLine();
			if(!response.equals("GOODBYE"))
			{
				System.out.println("\033[1;31m[����] >>\033[0m ����δ�����˳��� " + response);
				System.exit(-1);
			}
			else
			{
				System.out.println("\033[1;32m[�ɹ�] >>\033[0m �ɹ��ر�����");
			}

			in.close();
			out.close();
			socket.close();
		}
		catch (Exception e)
		{
			System.out.println("\033[1;31m[����] >>\033[0m "+e);
		}
	}
}
