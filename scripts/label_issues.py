import os
import json
import requests
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

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
    
    logging.debug(f"Retrieved {len(all_issues)} total issues")
    return all_issues

def add_label_to_issue(repo_owner, repo_name, issue_number, label):
    """Add a label to an issue"""
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues/{issue_number}/labels"
    headers = {
        "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    logging.info(f"Adding label '{label}' to issue #{issue_number}")
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
    
    logging.info(f"Updating issue #{issue_number} title to: {new_title}")
    response = requests.patch(url, json={"title": new_title}, headers=headers)
    if response.status_code != 200:
        raise Exception(f"Failed to update issue title: {response.status_code}")

def strip_existing_prefix(title):
    """Remove existing prefix if present and return the clean title"""
    logging.debug(f"Stripping prefix from title: {title}")
    
    if ':' in title:
        # Get everything after the first colon and strip whitespace
        clean_title = title.split(':', 1)[1].strip()
        
        # Remove any numbered prefix pattern (e.g., "001 - " or "002 -")
        if clean_title and clean_title[0].isdigit():
            try:
                # Try to remove the "XXX - " pattern if it exists
                parts = clean_title.split(' - ', 1)
                if len(parts) > 1 and parts[0].isdigit():
                    clean_title = parts[1]
            except (IndexError, ValueError):
                pass
        
        logging.debug(f"Cleaned title: {clean_title}")
        return clean_title
    return title

def get_prefix_from_labels(labels):
    """Determine the prefix from issue labels"""
    for label in labels:
        label_name = label['name'].lower()
        if label_name in LABEL_PREFIXES:
            prefix = LABEL_PREFIXES[label_name]
            logging.debug(f"Found prefix {prefix} from label {label_name}")
            return prefix
    logging.debug("No matching label found, using BUG prefix")
    return LABEL_PREFIXES['bug']

def reorder_issues(repo_owner, repo_name, prefix_to_update):
    """Reorder all issues with the given prefix"""
    logging.info(f"Reordering issues for prefix: {prefix_to_update}")
    
    issues = get_all_issues(repo_owner, repo_name)
    
    # Filter and sort issues by their current number for this prefix
    prefix_issues = []
    for issue in issues:
        title = issue.get("title", "")
        labels = issue.get("labels", [])
        
        # Check if this issue should have this prefix based on its labels
        should_have_prefix = False
        for label in labels:
            if LABEL_PREFIXES.get(label['name'].lower()) == prefix_to_update:
                should_have_prefix = True
                break
        
        if should_have_prefix:
            clean_title = strip_existing_prefix(title)
            prefix_issues.append({
                "number": issue["number"],
                "title": clean_title,
                "current_title": title
            })
            logging.debug(f"Including issue #{issue['number']} in reordering")
    
    logging.info(f"Found {len(prefix_issues)} issues to reorder for prefix {prefix_to_update}")
    
    # Update all issues with new sequential numbers
    for i, issue in enumerate(prefix_issues, 1):
        new_title = f"{prefix_to_update}-{i:03d}: {issue['title']}"
        if new_title != issue["current_title"]:
            update_issue_title(repo_owner, repo_name, issue["number"], new_title)
            logging.info(f"Updated issue #{issue['number']}: {issue['current_title']} -> {new_title}")

def main():
    logging.info("Starting issue processing")
    
    # Read the event data
    with open(os.getenv('GITHUB_EVENT_PATH')) as f:
        event = json.load(f)
    
    # Get repository information
    repo_owner, repo_name = os.getenv('GITHUB_REPOSITORY').split('/')
    issue_number = event['issue']['number']
    current_title = event['issue']['title']
    current_labels = event['issue']['labels']

    logging.info(f"Processing issue #{issue_number}: {current_title}")
    logging.info(f"Current labels: {[l['name'] for l in current_labels]}")

    # If no labels exist, add the "bug" label
    if not current_labels:
        logging.info(f"No labels found for issue #{issue_number}, adding 'bug' label")
        add_label_to_issue(repo_owner, repo_name, issue_number, "bug")
        current_labels = [{"name": "bug"}]

    # Get the prefix for this issue
    new_prefix = get_prefix_from_labels(current_labels)
    logging.info(f"Determined prefix {new_prefix} for issue #{issue_number}")
    
    # Get the old prefix if it exists
    old_prefix = None
    if ':' in current_title:
        try:
            old_prefix = current_title.split('-')[0]
            logging.info(f"Found old prefix {old_prefix} in title")
        except IndexError:
            pass
    
    # Always reorder issues for the current prefix type
    reorder_issues(repo_owner, repo_name, new_prefix)
    
    # If prefix changed, also reorder old prefix type
    if old_prefix and old_prefix != new_prefix:
        logging.info(f"Prefix changed from {old_prefix} to {new_prefix}, reordering both")
        reorder_issues(repo_owner, repo_name, old_prefix)
    
    logging.info(f"Completed processing for issue #{issue_number}")

if __name__ == "__main__":
    main()