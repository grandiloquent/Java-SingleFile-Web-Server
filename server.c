
// -lws2_32
#ifdef _WIN32
#include <winsock2.h>
#endif

#include <stdio.h>

void not_found(int client) {}
int startup(u_short *port, const char *ip) {
#ifdef _WIN32
  WSADATA wsaData = {0};
  int result = WSAStartup(MAKEWORD(2, 2), &wsaData);
  if (result != 0) {
    printf("[Failed]: WSAStartup. result = %d.\n", result);
    return -1;
  }

#endif
  // https://docs.microsoft.com/en-us/windows/desktop/api/winsock/ns-winsock-sockaddr_in
  struct sockaddr_in name;
  int addrlen = sizeof(name);
  // https://docs.microsoft.com/en-us/windows/desktop/api/winsock2/nf-winsock2-socket
  int httpd = socket(AF_INET, SOCK_STREAM, 0);
  if (httpd < 0) {
#ifdef _WIN32
    printf("[Failed]: socket. %I64d\n", INVALID_SOCKET);
#endif
    return -1;
  }
  memset(&name, 0, sizeof(name));
  name.sin_family = AF_INET;
  // https://docs.microsoft.com/en-us/windows/desktop/api/winsock/nf-winsock-htons
  name.sin_port = htons(*port);
  // https://docs.microsoft.com/en-us/windows/desktop/api/winsock/nf-winsock-htonl
  name.sin_addr.s_addr = inet_addr(ip); // htonl(INADDR_ANY);
  if (bind(httpd, (struct sockaddr *)&name, addrlen) < 0) {
    puts("[Failed]: bind");
    return -1;
  }
  //   getsockname(httpd, (struct sockaddr *)&name, &addrlen); // read binding

  //   unsigned short local_port = ntohs(name.sin_port);

  //   char *ip = inet_ntoa(name.sin_addr);
  //   printf("ip: %s, port: %d\n", ip, local_port);
  if (*port == 0) {
    int namelen = sizeof(name);
    if (getsockname(httpd, (struct sockaddr *)&name, &namelen) == -1) {
      puts("[Failed]: getsockname");
    }
    *port = ntohs(name.sin_port);
  }

  if (listen(httpd, 5) < 0) {
    puts("[Failed]: listen");
    return httpd;
  }
  return httpd;
}
void accpet_request(int client) {}
int main(int argc, char *argv[]) {
  u_short port = 8090;
  int server = startup(&port, "127.0.0.1");
  int client = -1;
  struct sockaddr_in client_name;
  int client_name_len = sizeof(client_name);

  while (1) {
    // https://docs.microsoft.com/en-us/windows/desktop/api/winsock/ns-winsock-sockaddr
    client = accept(server, (struct sockaddr *)&client_name, &client_name_len);
    puts("accept");
    if (client == -1) {
      accpet_request(client);
    }
  }
  // close(server);
#ifdef _WIN32
  WSACleanup();
#endif
  return 0;
}
