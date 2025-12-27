import json
import matplotlib.pyplot as plt
import argparse
import os

# This script utilizes matplotlib to plot the performance of a given
# Nocker portscan. It utilizes the durationMillis value reported by
# the given task objects from a json report.
# invocation ex: python performance_chart.py reports/scan_me_nmap_org_t100_c300.json reports/scan_me_nmap_org_t100_c300.png
def plot_durations(input_file: str, output_file: str):
    if not os.path.isfile(input_file):
        raise FileNotFoundError(f"Input file does not exist: {input_file}")

    with open(input_file) as f:
        data = json.load(f)

    tasks = data.get('tasks', [])
    if not tasks:
        raise ValueError("No tasks found in JSON data")

    durations = [task.get('durationMillis', 0) for task in tasks]

    plt.figure(figsize=(10, 6))
    plt.plot(durations, marker='o')
    plt.xlabel("Port index")
    plt.ylabel("Duration (ms)")
    plt.title("Port Scan Duration Timeline")
    plt.grid(True)

    plt.savefig(output_file)
    plt.close()
    print(f"Plot saved to {output_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Plot port scan durations from JSON")
    parser.add_argument("input_file", help="Path to input JSON file")
    parser.add_argument("output_file", help="Path to output image file (e.g., .png)")
    args = parser.parse_args()

    plot_durations(args.input_file, args.output_file)

