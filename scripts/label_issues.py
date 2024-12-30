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

def get_existing_numbered_label(labels):
    """Check if the issue already has a numbered label"""
    for label in labels:
        name = label["name"]
        for prefix in LABEL_PREFIXES.values():
            if name.startswith(prefix + "-"):
                return name
    return None

def remove_label(repo_owner, repo_name, issue_number, label):
    """Remove a label from an issue"""
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues/{issue_number}/labels/{label}"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    response = requests.delete(url, headers=headers)
    if response.status_code not in [200, 404]:  # 404 means label was already removed
        raise Exception(f"Failed to remove label: {response.status_code}")

def get_next_label(repo_owner, repo_name, label_prefix):
    """Get the next available number for a label prefix"""
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    # Get all issues with their labels
    response = requests.get(url, headers=headers, params={"state": "all", "per_page": 100})
    if response.status_code != 200:
        raise Exception(f"Failed to fetch issues: {response.status_code}")

    issues = response.json()
    
    # Find the highest number for this prefix
    max_number = 0
    for issue in issues:
        for label in issue.get("labels", []):
            if label["name"].startswith(label_prefix):
                try:
                    number = int(label["name"].split('-')[1])
                    max_number = max(max_number, number)
                except (IndexError, ValueError):
                    continue
    
    # Generate next label
    return f"{label_prefix}-{(max_number + 1):03d}"

def add_label_to_issue(repo_owner, repo_name, issue_number, label):
    """Add a label to an issue"""
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues/{issue_number}/labels"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    response = requests.post(url, json={"labels": [label]}, headers=headers)
    if response.status_code != 200:
        raise Exception(f"Failed to add label: {response.status_code}")

def main():
    # Read the event data
    with open(os.getenv('GITHUB_EVENT_PATH')) as f:
        event = json.load(f)
    
    # Get repository information
    repo_owner, repo_name = os.getenv('GITHUB_REPOSITORY').split('/')
    issue_number = event['issue']['number']
    current_labels = event['issue']['labels']
    
    # Check for existing numbered label
    existing_numbered_label = get_existing_numbered_label(current_labels)
    
    # Determine the issue type from existing labels
    issue_type = "bug"  # default type
    for label in current_labels:
        label_name = label['name'].lower()
        if label_name in LABEL_PREFIXES:
            issue_type = label_name
            break
    
    # Get the prefix for this type
    prefix = LABEL_PREFIXES[issue_type]
    
    # If the issue type has changed, remove old numbered label and add new one
    if existing_numbered_label:
        old_prefix = existing_numbered_label.split('-')[0]
        if old_prefix != prefix:
            remove_label(repo_owner, repo_name, issue_number, existing_numbered_label)
            next_label = get_next_label