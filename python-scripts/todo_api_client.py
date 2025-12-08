import requests
import sys

BASE_URL = "http://tomcat:8080/todoapp/todos"

def list_todos():
    resp = requests.get(BASE_URL)
    resp.raise_for_status()
    data = resp.json()
    for todo in data:
        print(f"{todo['id']}. {todo['description']} [{'x' if todo['completed'] else ' '}]")

def add_todo(description):
    resp = requests.post(BASE_URL, json={"description": description})
    if resp.status_code == 201:
        print("Added successfully.")
    else:
        print(f"Error: {resp.status_code} {resp.text}")

def edit_todo(id, description=None, completed=None):
    url = f"{BASE_URL}/{id}"
    payload = {}
    if description:
        payload["description"] = description
    if completed is not None:
        payload["completed"] = completed
    if not payload:
        print("Nothing to update.")
        return
    resp = requests.put(url, json=payload)
    if resp.status_code == 200:
        print(f"Updated task {id}")
    elif resp.status_code == 404:
        print(f"Task {id} not found")
    else:
        print(f"Error: {resp.status_code} {resp.text}")

def delete_todo(id):
    url = f"{BASE_URL}/{id}"
    resp = requests.delete(url)
    if resp.status_code == 204:
        print(f"Deleted task {id}")
    elif resp.status_code == 404:
        print(f"Task {id} not found")
    else:
        print(f"Error: {resp.status_code} {resp.text}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python todo_api_client.py [list|add <desc>|edit <id> [desc] [--done|--notdone]|delete <id>]")
        sys.exit(1)
    cmd = sys.argv[1]
    if cmd == 'list':
        list_todos()
    elif cmd == 'add':
        desc = ' '.join(sys.argv[2:])
        add_todo(desc)
    elif cmd == 'edit':
        if len(sys.argv) < 3:
            print("edit requires at least id")
            sys.exit(1)
        id = sys.argv[2]
        desc = None
        completed = None
        rest = sys.argv[3:]
        for arg in rest:
            if arg == "--done":
                completed = True
            elif arg == "--notdone":
                completed = False
            else:
                desc = arg
        edit_todo(id, desc, completed)
    elif cmd == 'delete':
        if len(sys.argv) < 3:
            print("delete requires id")
            sys.exit(1)
        delete_todo(sys.argv[2])
    else:
        print("Unknown command")