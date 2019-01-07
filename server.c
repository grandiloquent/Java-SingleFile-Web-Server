
// -lws2_32
#ifdef _WIN32
#include <winsock2.h>
#endif

#include <stdio.h>
#include <ctype.h>
#include <dirent.h>
#include <sys/stat.h>

#define BUFFER_SIZE 1024
#define BUFFER_SIZE_METHOD 10

void accpet_request(int client);
void not_found(int client);
int startup(u_short *port, const char *ip);

int get_line(int client, char *buf, int size) {
  int i = 0;
  char c = '\0';
  int n;
  while ((i < size - 1) && (c != '\n')) {
    // https://docs.microsoft.com/en-us/windows/desktop/api/winsock/nf-winsock-recv
    n = recv(client, &c, 1, 0);
    if (n > 0) {
      if (c == '\r') {
        n = recv(client, &c, 1, MSG_PEEK);
        if ((n > 0) && (c == '\n'))
          recv(client, &c, 1, 0);
        else
          c = '\n';
      }
      buf[i] = c;
      i++;
    } else
      c = '\n';
  }
  buf[i] = '\0';
  return i;
}
void accpet_request(int client) {
  char buf[BUFFER_SIZE];
  int numchars;

  numchars = get_line(client, buf, BUFFER_SIZE);

  size_t i = 0, j = 0;
  char method[BUFFER_SIZE_METHOD];
  while (!isspace(buf[j]) && (i < BUFFER_SIZE_METHOD - 1)) {
    method[i] = buf[j];
    i++;
    j++;
  }
  method[i] = '\0';
  if (strcasecmp(method, "GET") && strcasecmp(method, "POST")) {
    printf("Dont support the method = %s.\n", method);
    return;
  }
  printf("%s\n", method);
}
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
typedef struct _File {
  char *path;
  size_t time;
} File;

File *list_files(const char *path) {
  DIR *dir;
  struct dirent *ent;
  File *files = malloc(100 * sizeof(File));
  int index = 0;
  struct stat s;

  if ((dir = opendir(path)) != NULL) {
    while ((ent = readdir(dir)) != NULL) {
      if (strcmp(ent->d_name, ".") == 0 || strcmp(ent->d_name, "..") == 0)
        continue;
      char p[MAX_PATH];
      strcpy(p, path);
      strcat(p, "/");
      strcat(p, ent->d_name);

      stat(p, &s);
      if (S_ISDIR(s.st_mode)) {
        list_files(p);
      } else {
        if (index < 100) {
          File file;
          char *buf = malloc(MAX_PATH * sizeof(char));
          strcpy(buf, p);
          file.path = buf;
          file.time = 123;
          files[index++] = file;
        } else {
          break;
        }
      }
    }

    closedir(dir);
  }
  return files;
}

int main(int argc, char *argv[]) {
  File *files = list_files("C:\\Users\\psycho\\Downloads");
  printf("%d\n", sizeof(files));
  int index = 0;
  while (index++ < 100) {
    printf("%s\n", files->path);
    files++;
  }
  //   u_short port = 8090;
  //   int server = startup(&port, "127.0.0.1");
  //   int client = -1;
  //   struct sockaddr_in client_name;
  //   int client_name_len = sizeof(client_name);
  //   while (1) {
  //     //
  //     https://docs.microsoft.com/en-us/windows/desktop/api/winsock/ns-winsock-sockaddr
  //     client = accept(server, (struct sockaddr *)&client_name,
  //     &client_name_len); puts("accept"); if (client == -1) {
  //     }
  //     accpet_request(client);
  //   }
  //   // close(server);
  // #ifdef _WIN32
  //   WSACleanup();
  // #endif
  //   return 0;
}