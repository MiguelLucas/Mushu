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

def get_next_number(repo_owner, repo_name, prefix):
    """Get the next available number for a prefix"""
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    response = requests.get(url, headers=headers, params={"state": "all", "per_page": 100})
    if response.status_code != 200:
        raise Exception(f"Failed to fetch issues: {response.status_code}")

    issues = response.json()
    
    max_number = 0
    for issue in issues:
        title = issue.get("title", "")
        if title.startswith(f"{prefix}-"):
            try:
                number = int(title.split(':')[0].split('-')[1])
                max_number = max(max_number, number)
            except (IndexError, ValueError):
                continue
    
    return max_number + 1

def update_issue_title(repo_owner, repo_name, issue_number, new_title):
    """Update the issue title"""
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues/{issue_number}"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    response = requests.patch(url, json={"title": new_title}, headers=headers)
    if response.status_code != 200:
        raise Exception(f"Failed to update issue title: {response.status_code}")

def strip_existing_prefix(title):
    """Remove existing prefix if present and return the clean title"""
    if ':' in title:
        # Split on first colon and take everything after it
        clean_title = title.split(':', 1)[1].strip()
        return clean_title
    return title

def main():
    # Read the event data
    with open(os.getenv('GITHUB_EVENT_PATH')) as f:
        event = json.load(f)
    
    # Get repository information
    repo_owner, repo_name = os.getenv('GITHUB_REPOSITORY').split('/')
    issue_number = event['issue']['number']
    current_title = event['issue']['title']
    current_labels = event['issue']['labels']

    # Clean the title by removing any existing prefix
    clean_title = strip_existing_prefix(current_title)

    # If no labels exist, add the "bug" label
    if not current_labels:
        print("No labels found, adding 'bug' label")
        add_label_to_issue(repo_owner, repo_name, issue_number, "bug")
        issue_type = "bug"
    else:
        # Determine the issue type from existing labels
        issue_type = "bug"  # default type
        for label in current_labels:
            label_name = label['name'].lower()
            if label_name in LABEL_PREFIXES:
                issue_type = label_name
                break
    
    # Get the prefix for this type
    prefix = LABEL_PREFIXES[issue_type]
    
    # Get the next number
    next_number = get_next_number(repo_owner, repo_name, prefix)
    
    # Create new title with prefix
    new_title = f"{prefix}-{next_number:03d}: {clean_title}"
    
    # Update the issue only if the title would change
    if new_title != current_title:
        update_issue_title(repo_owner, repo_name, issue_number, new_title)
        print(f"Updated issue #{issue_number} title to: {new_title}")
    else:
        print(f"No title update needed for issue #{issue_number}")

if __name__ == "__main__":
    main()