import os
import json
import requests

# Map the label types to their prefixes
LABEL_PREFIXES = {
    "bug": "BUG",
    "test": "TEST",
    "feature": "FEAT",
    "documentation": "DOCS",
    "ops": "OPS",
    "duplicate": "DUP",
}

def get_next_label(repo_owner, repo_name, label_prefix):
    # GitHub API URL to fetch issues
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}"
    }

    # Get all issues to check for the latest label number
    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        raise Exception(f"Failed to fetch issues: {response.status_code}")

    issues = response.json()

    # Extract labels and find the latest label number for the given prefix
    existing_labels = []
    for issue in issues:
        for label in issue.get("labels", []):
            if label["name"].startswith(label_prefix):
                existing_labels.append(label["name"])

    # Get the highest label number and increment it
    existing_labels = sorted(existing_labels, key=lambda x: int(x.split('-')[1]) if x.split('-')[0] == label_prefix else 0)
    latest_label = existing_labels[-1] if existing_labels else f"{label_prefix}-000"
    latest_number = int(latest_label.split('-')[1])
    next_number = latest_number + 1
    next_label = f"{label_prefix}-{next_number:03d}"

    return next_label

def add_label_to_issue(repo_owner, repo_name, issue_number, next_label):
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues/{issue_number}/labels"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
        "Content-Type": "application/json"
    }
    payload = {
        "labels": [next_label]
    }

    # Add the label to the issue
    response = requests.post(url, json=payload, headers=headers)
    if response.status_code == 200:
        print(f"Successfully added label {next_label} to issue #{issue_number}")
    else:
        print(f"Failed to add label: {response.status_code}, {response.text}")

def main():
    # Get repository details and issue number from environment variables
    repo_owner, repo_name = os.getenv('GITHUB_REPOSITORY').split('/')
    issue_number = os.getenv('GITHUB_REF').split('/')[-1]

    # Get the issue labels from the event payload
    issue_labels = os.getenv("GITHUB_EVENT_PATH")
    with open(issue_labels) as f:
        event_data = json.load(f)

    # Extract the first label type from the issue or use "bug" as default
    label_name = "bug"  # Default label
    if event_data.get("labels"):
        first_label = event_data["labels"][0]["name"].lower()
        if first_label in LABEL_PREFIXES:
            label_name = first_label

    label_prefix = LABEL_PREFIXES[label_name]
    
    # Get the next label for this prefix
    next_label = get_next_label(repo_owner, repo_name, label_prefix)

    # Add the label to the issue
    add_label_to_issue(repo_owner, repo_name, issue_number, next_label)

if __name__ == "__main__":
    main()
