// Documentation: https://developer.mozilla.org/en-US/docs/Web/API/WebSocket

// // Create WebSocket connection.
// const socket = new WebSocket("ws://localhost:9000/ws");

// // Connection opened
// socket.addEventListener("open", (event) => {
//     document.getElementById("status").innerHTML = "Connected";
// });

// socket.addEventListener("close", (event) => {
//     document.getElementById("status").innerHTML = "Disconnected";
// });

// // Listen for messages
// socket.addEventListener("message", (event) => {
//     var timeStr = document.createElement("li");
//     timeStr.textContent = event.data;
//     document.getElementById("time-tick").appendChild(timeStr);
// });

// const socket = new WebSocket(`ws://${window.location.host}/ws`);

// socket.onopen = () => {
// 	console.log("WebSocket connection established");
// };

// socket.onmessage = (event) => {
// 	const data = JSON.parse(event.data);

// 	console.log(data);
// 	switch (data.status) {
// 		case "started":
// 			showStatus("Analysis started...");
// 			break;

// 		case "searching":
// 			break;

// 		case "completed":
// 			console.log(data);
// 			showVideos(data.data.items);
// 			showSentimentResult(data.data);
// 			break;
// 	}
// };

// function startAnalysis(query) {
// 	socket.send(
// 		JSON.stringify({
// 			action: "search",
// 			query: query,
// 		}),
// 	);
// }

// function showStatus(message) {
// 	document.getElementById("status").textContent = message;
// }

// function showVideos(videos) {
// 	const videoList = document.getElementById("videoList");
// 	videoList.innerHTML = videos
// 		.map(
// 			(video) => `
//         <div class="video-item">
//             <img src="${video.thumbnailUrl}" alt="${video.title}">
//             <h3>${video.title}</h3>
//             <p>${video.description}</p>
//         </div>
//     `,
// 		)
// 		.join("");
// }

// function showSentimentResult(result) {
// 	document.getElementById("sentiment").textContent =
// 		`Sentiment: ${result.sentiment} (Score: ${result.score})`;
// }
//

const socket = new WebSocket(`ws://${window.location.host}/ws`);
let keepAliveInterval;

socket.onopen = () => {
	console.log("WebSocket connection established");
	startKeepAlive();
};

socket.onmessage = (event) => {
	const data = JSON.parse(event.data);
	console.log("Received data:", data);

	$("#results").empty()

	for (let i = 0; i < data.responses.length; i++) {
		// console.log(data.responses[i]);
		displayResults(data.responses[i]);
	}

	// switch (data.status) {
	// 	case "started":
	// 		$("#results").empty().append("<h2>Searching...</h2>");
	// 		break;
	//
	// 	case "searching":
	// 		break;
	//
	// 	case "completed":
	// 		displayResults(data.data);
	// 		break;
	//
	// 	case "error":
	// 		$("#results").empty().append(`<h2>Error: ${data.data.error}</h2>`);
	// 		break;
	// }
};

function startKeepAlive() {
	keepAliveInterval = setInterval(() => {
		if (socket.readyState === WebSocket.OPEN) {
			socket.send(JSON.stringify({ action: "ping" }));
		}
	}, 25000); // Send a ping every 25 seconds
}

function stopKeepAlive() {
	clearInterval(keepAliveInterval);
}

function searchVideos() {
	const query = $("#searchQuery").val();
	if (!query) {
		alert("Please enter a search term");
		return;
	}

	socket.send(
		JSON.stringify({
			action: "search",
			query: query,
		}),
	);
}

function displayResults(data) {
	// $("#results").empty();

	// Add search header and stats
	$("#results").append(`
        <h2>Search term: ${data.query}</h2>
        <b>Word Stats:</b><a id="moreStats" href="/wordstats/${encodeURIComponent($("#searchQuery").val())}">More Stats</a><br>
        <body>
            <b>Sentiment:</b> ${data.sentiment || "N/A"}<br>
            <b>Flesh-Kincaid Grade Level Avg:</b> ${data.fleschKincaidGradeLevelAvg || "N/A"}<br>
            <b>Flesch Reading Ease Score Avg:</b> ${data.fleschReadingScoreAvg || "N/A"}
        </body>
    `);

	// Add video items
	data.items.forEach((item) => {
		const videoId = item.id.videoId;
		const snippet = item.snippet;
		const html = `
            <div class="video-item">
                <h3><p>Title: <a href="https://www.youtube.com/watch?v=${videoId}" target="_blank">${snippet.title}</a></p></h3>
                <h3><p>Channel: <a href="/channel/${encodeURIComponent(snippet.channelId)}" target="_blank">${snippet.channelTitle}</a></p></h3>
                <img class="thumbnail" style="top: 0; right: 0; width: 100px; height: auto;" src="${snippet.thumbnails.default.url}">
                <h3><p>Description:</h3> ${snippet.description}</p>
                <b><p>Flesch-Kincaid Grade Level:</b> ${item.fleschKincaidGradeLevel || "N/A"}</p>
                <b><p>Flesch Reading Ease Score:</b> ${item.fleschReadingScore || "N/A"}</p>
                <a href="/tag?video_id=${videoId}" target="_blank">Tags</a>
            </div>
        `;
		$("#results").append(html);
	});
}

// Handle WebSocket errors and closure
socket.onerror = (error) => {
	console.error("WebSocket error:", error);
	$("#results").empty().append("<h2>Error connecting to server</h2>");
	stopKeepAlive();
};

socket.onclose = (event) => {
	console.log("WebSocket connection closed", event);
	if (!event.wasClean) {
		$("#results").empty().append("<h2>Lost connection to server</h2>");
	}
	stopKeepAlive();
};

// Cleanup on page unload
window.addEventListener("beforeunload", () => {
	if (socket && socket.readyState === WebSocket.OPEN) {
		socket.close();
	}
});
