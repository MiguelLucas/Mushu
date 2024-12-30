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

def get_all_issues(repo_owner, repo_name):
    """Get all issues from the repository with pagination"""
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    all_issues = []
    page = 1
    
    while True:
        response = requests.get(
            url, 
            headers=headers, 
            params={"state": "all", "per_page": 100, "page": page}
        )
        if response.status_code != 200:
            raise Exception(f"Failed to fetch issues: {response.status_code}")
        
        issues = response.json()
        if not issues:
            break
            
        all_issues.extend(issues)
        page += 1
    
    return all_issues

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
        clean_title = title.split(':', 1)[1].strip()
        return clean_title
    return title

def get_prefix_from_labels(labels):
    """Determine the prefix from issue labels"""
    for label in labels:
        label_name = label['name'].lower()
        if label_name in LABEL_PREFIXES:
            return LABEL_PREFIXES[label_name]
    return LABEL_PREFIXES['bug']  # default to BUG

def reorder_issues(repo_owner, repo_name, prefix_to_update):
    """Reorder all issues with the given prefix"""
    issues = get_all_issues(repo_owner, repo_name)
    
    # Filter and sort issues by their current number for this prefix
    prefix_issues = []
    for issue in issues:
        title = issue.get("title", "")
        if title.startswith(f"{prefix_to_update}-"):
            try:
                current_number = int(title.split(':')[0].split('-')[1])
                clean_title = strip_existing_prefix(title)
                prefix_issues.append({
                    "number": issue["number"],
                    "title": clean_title,
                    "current_number": current_number
                })
            except (IndexError, ValueError):
                continue
    
    # Sort by current number
    prefix_issues.sort(key=lambda x: x["current_number"])
    
    # Update all issues with new sequential numbers
    for i, issue in enumerate(prefix_issues, 1):
        new_title = f"{prefix_to_update}-{i:03d}: {issue['title']}"
        update_issue_title(repo_owner, repo_name, issue["number"], new_title)
        print(f"Reordered issue #{issue['number']} to: {new_title}")

def main():
    # Read the event data
    with open(os.getenv('GITHUB_EVENT_PATH')) as f:
        event = json.load(f)
    
    # Get repository information
    repo_owner, repo_name = os.getenv('GITHUB_REPOSITORY').split('/')
    issue_number = event['issue']['number']
    current_title = event['issue']['title']
    current_labels = event['issue']['labels']

    # If no labels exist, add the "bug" label
    if not current_labels:
        print("No labels found, adding 'bug' label")
        add_label_to_issue(repo_owner, repo_name, issue_number, "bug")
        current_labels = [{"name": "bug"}]

    # Get the prefix for this issue
    new_prefix = get_prefix_from_labels(current_labels)
    
    # Get the old prefix if it exists
    old_prefix = None
    if ':' in current_title:
        old_prefix = current_title.split('-')[0]
    
    # Clean the title
    clean_title = strip_existing_prefix(current_title)
    
    # Reorder issues for both old and new prefixes if they're different
    if old_prefix and old_prefix != new_prefix:
        reorder_issues(repo_owner, repo_name, old_prefix)
    reorder_issues(repo_owner, repo_name, new_prefix)
    
    print(f"Completed reordering for issue #{issue_number}")

if __name__ == "__main__":
    main()